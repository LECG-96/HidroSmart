package com.devst.hidrosmart.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.devst.hidrosmart.R;

/** Indicador gráfico de un actuador o del nivel medido del estanque. */
public final class IndicadorSistemaView extends View {
    public static final int VENTILADOR = 0;
    public static final int LUZ = 1;
    public static final int BOMBA = 2;
    public static final int CERRADURA = 3;
    public static final int ESTANQUE = 4;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private int tipo = BOMBA;
    private boolean activo;
    private float nivel = 0f;
    private float nivelObjetivo = 0f;
    private float fase;
    private float giro;
    private boolean adjunto;
    private boolean animacionesHabilitadas = true;
    private ValueAnimator animadorNivel;
    private final Runnable animar = new Runnable() {
        @Override public void run() {
            if (!adjunto || !animacionesHabilitadas) return;
            fase = (fase + 0.08f) % 1f;
            if (tipo == VENTILADOR) giro = (giro + 9f) % 360f;
            invalidate();
            if (necesitaAnimacionContinua()) postOnAnimation(this);
        }
    };

    public IndicadorSistemaView(Context context) { this(context, null); }
    public IndicadorSistemaView(Context context, @Nullable AttributeSet attrs) { this(context, attrs, 0); }
    public IndicadorSistemaView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void configurar(int tipo, boolean activo) {
        this.tipo = tipo;
        this.activo = activo;
        iniciarOPararAnimacion();
        invalidate();
    }

    public void configurarNivel(float porcentaje) {
        tipo = ESTANQUE;
        float nuevo = Math.max(0f, Math.min(100f, porcentaje));
        nivelObjetivo = nuevo / 100f;
        if (!animacionesHabilitadas || !adjunto || Math.abs(nivelObjetivo - nivel) < 0.02f) {
            if (animadorNivel != null) animadorNivel.cancel();
            nivel = nivelObjetivo;
            invalidate();
            return;
        }
        if (animadorNivel != null) animadorNivel.cancel();
        animadorNivel = ValueAnimator.ofFloat(nivel, nivelObjetivo);
        animadorNivel.setDuration(650);
        animadorNivel.addUpdateListener(a -> {
            nivel = (float) a.getAnimatedValue();
            invalidate();
        });
        animadorNivel.start();
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        adjunto = true;
        iniciarOPararAnimacion();
    }

    @Override protected void onDetachedFromWindow() {
        adjunto = false;
        removeCallbacks(animar);
        if (animadorNivel != null) animadorNivel.cancel();
        super.onDetachedFromWindow();
    }

    private boolean animacionPermitida() {
        try {
            return ValueAnimator.areAnimatorsEnabled()
                    && Settings.Global.getFloat(getContext().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        } catch (RuntimeException error) {
            return true;
        }
    }

    private boolean necesitaAnimacionContinua() {
        return activo && (tipo == VENTILADOR || tipo == LUZ || tipo == BOMBA || tipo == CERRADURA);
    }

    private void iniciarOPararAnimacion() {
        removeCallbacks(animar);
        animacionesHabilitadas = animacionPermitida();
        if (adjunto && animacionesHabilitadas && necesitaAnimacionContinua()) postOnAnimation(animar);
    }

    private int color(int resource) { return ContextCompat.getColor(getContext(), resource); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float escala = Math.min(getWidth(), getHeight()) / 56f;
        canvas.save();
        canvas.translate(cx, cy);
        canvas.scale(escala, escala);
        paint.setStrokeWidth(2.2f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color(activo || tipo == ESTANQUE ? R.color.verde_primario : R.color.texto_terciario));
        switch (tipo) {
            case VENTILADOR: dibujarVentilador(canvas); break;
            case LUZ: dibujarLuz(canvas); break;
            case CERRADURA: dibujarCerradura(canvas); break;
            case ESTANQUE: dibujarEstanque(canvas); break;
            default: dibujarBomba(canvas); break;
        }
        canvas.restore();
    }

    private void dibujarVentilador(Canvas canvas) {
        canvas.save(); canvas.rotate(activo && animacionesHabilitadas ? giro : 0f);
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 3; i++) {
            canvas.save(); canvas.rotate(i * 120f);
            Path pala = new Path();
            pala.moveTo(0, -3); pala.cubicTo(2, -7, 5, -20, 0, -22);
            pala.cubicTo(-7, -17, -8, -8, -3, -2); pala.close();
            paint.setColor(color(activo ? R.color.verde_primario : R.color.texto_terciario));
            canvas.drawPath(pala, paint); canvas.restore();
        }
        paint.setColor(color(R.color.superficie_tarjeta)); canvas.drawCircle(0, 0, 3, paint);
        canvas.restore();
    }

    private void dibujarLuz(Canvas canvas) {
        float brillo = activo && animacionesHabilitadas ? (float) (0.72 + 0.28 * Math.sin(fase * Math.PI * 2)) : 1f;
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color(activo ? R.color.verde_acento : R.color.texto_terciario));
        paint.setAlpha((int) (255 * brillo));
        canvas.drawCircle(0, 0, 17, paint);
        for (int i = 0; i < 8; i++) {
            canvas.save(); canvas.rotate(i * 45f);
            canvas.drawLine(0, -22, 0, -27, paint); canvas.restore();
        }
        paint.setAlpha(255);
        if (activo) {
            paint.setStyle(Paint.Style.FILL); paint.setColor(color(R.color.verde_acento));
            canvas.drawCircle(0, 0, 10, paint);
        }
    }

    private void dibujarBomba(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        rect.set(-17, -11, 12, 11); canvas.drawRoundRect(rect, 4, 4, paint);
        canvas.drawLine(12, -5, 21, -5, paint); canvas.drawLine(12, 5, 21, 5, paint);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(-5, 0, 5, paint);
        if (activo) {
            paint.setColor(color(R.color.verde_primario));
            float y = -17 + ((fase * 24f) % 24f);
            Path gota = new Path(); gota.moveTo(23, y - 3); gota.quadTo(17, y + 5, 23, y + 7);
            gota.quadTo(29, y + 5, 23, y - 3); canvas.drawPath(gota, paint);
        }
    }

    private void dibujarCerradura(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        rect.set(-14, -3, 14, 18); canvas.drawRoundRect(rect, 3, 3, paint);
        canvas.save();
        if (activo && animacionesHabilitadas) {
            float apertura = (float) Math.sin(fase * Math.PI * 2) * 18f;
            canvas.rotate(apertura, -9, 3);
        }
        rect.set(-9, -19, 9, 3); canvas.drawArc(rect, 180, 180, false, paint);
        canvas.restore();
        paint.setStyle(Paint.Style.FILL); canvas.drawCircle(0, 5, 2.4f, paint);
        canvas.drawRect(-1, 6, 1, 11, paint);
    }

    private void dibujarEstanque(Canvas canvas) {
        paint.setStyle(Paint.Style.STROKE);
        rect.set(-20, -24, 20, 24); canvas.drawRoundRect(rect, 4, 4, paint);
        float aguaY = 20 - 40 * nivel;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color(R.color.verde_claro));
        rect.set(-17, aguaY, 17, 21); canvas.drawRect(rect, paint);
        paint.setColor(color(R.color.verde_primario));
        canvas.drawLine(-17, aguaY, 17, aguaY, paint);
        paint.setStyle(Paint.Style.STROKE); paint.setColor(color(R.color.verde_primario));
        rect.set(-20, -24, 20, 24); canvas.drawRoundRect(rect, 4, 4, paint);
    }
}
