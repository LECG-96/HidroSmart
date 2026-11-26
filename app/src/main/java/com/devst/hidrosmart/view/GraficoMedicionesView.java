package com.devst.hidrosmart.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.modelo.MedicionMuestra;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Gráfico liviano de tendencias, sin una dependencia externa de charting. */
public class GraficoMedicionesView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<MedicionMuestra> muestras;
    private String mensajeVacio = "Esperando mediciones";

    public GraficoMedicionesView(Context context) {
        super(context);
        inicializar();
    }

    public GraficoMedicionesView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        inicializar();
    }

    public GraficoMedicionesView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        inicializar();
    }

    private void inicializar() {
        setMinimumHeight(dp(180));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    public void setMuestras(List<MedicionMuestra> muestras) {
        this.muestras = muestras;
        invalidate();
    }

    public void setMensajeVacio(String mensaje) {
        mensajeVacio = mensaje;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float left = dp(48);
        float right = getWidth() - dp(12);
        float top = dp(14);
        float bottom = getHeight() - dp(30);
        if (right <= left || bottom <= top) return;

        if (muestras == null || muestras.isEmpty()) {
            paint.setColor(ContextCompat.getColor(getContext(), R.color.texto_secundario));
            paint.setTextSize(dp(13));
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(mensajeVacio, getWidth() / 2f, getHeight() / 2f, paint);
            return;
        }

        double minimo = Double.MAX_VALUE;
        double maximo = -Double.MAX_VALUE;
        for (MedicionMuestra muestra : muestras) {
            minimo = Math.min(minimo, muestra.valor);
            maximo = Math.max(maximo, muestra.valor);
        }
        if (Math.abs(maximo - minimo) < 0.0001) {
            double margen = Math.max(0.5, Math.abs(maximo) * 0.05);
            minimo -= margen;
            maximo += margen;
        } else {
            double margen = (maximo - minimo) * 0.12;
            minimo -= margen;
            maximo += margen;
        }

        paint.setStrokeWidth(dp(1));
        paint.setTextSize(dp(10));
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.texto_terciario));
        for (int i = 0; i <= 3; i++) {
            float y = bottom - (bottom - top) * i / 3f;
            paint.setColor(ContextCompat.getColor(getContext(), R.color.divisor));
            canvas.drawLine(left, y, right, y, paint);
            double valorGuia = minimo + (maximo - minimo) * i / 3.0;
            paint.setColor(ContextCompat.getColor(getContext(), R.color.texto_terciario));
            canvas.drawText(String.format(Locale.getDefault(), "%.1f", valorGuia), left - dp(6), y + dp(4), paint);
        }

        float rangoX = Math.max(1f, right - left);
        float rangoY = Math.max(1f, bottom - top);
        long inicio = muestras.get(0).fechaMillis;
        long fin = muestras.get(muestras.size() - 1).fechaMillis;
        long duracion = Math.max(1L, fin - inicio);
        Path linea = new Path();
        for (int i = 0; i < muestras.size(); i++) {
            MedicionMuestra muestra = muestras.get(i);
            float x = left + rangoX * ((muestra.fechaMillis - inicio) / (float) duracion);
            float y = bottom - rangoY * (float) ((muestra.valor - minimo) / (maximo - minimo));
            if (i == 0) linea.moveTo(x, y);
            else linea.lineTo(x, y);
        }
        paint.setColor(ContextCompat.getColor(getContext(), R.color.verde_primario));
        paint.setStrokeWidth(dp(2.5f));
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawPath(linea, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(ContextCompat.getColor(getContext(), R.color.verde_primario));
        for (MedicionMuestra muestra : muestras) {
            float x = left + rangoX * ((muestra.fechaMillis - inicio) / (float) duracion);
            float y = bottom - rangoY * (float) ((muestra.valor - minimo) / (maximo - minimo));
            canvas.drawCircle(x, y, dp(2.5f), paint);
        }

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(dp(10));
        paint.setColor(ContextCompat.getColor(getContext(), R.color.texto_terciario));
        SimpleDateFormat formato = new SimpleDateFormat("HH:mm", Locale.getDefault());
        canvas.drawText(formato.format(new Date(inicio)), left, getHeight() - dp(8), paint);
        String etiquetaFin = formato.format(new Date(fin));
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(etiquetaFin, right, getHeight() - dp(8), paint);

        MedicionMuestra ultimo = muestras.get(muestras.size() - 1);
        setContentDescription(String.format(Locale.getDefault(),
                "Tendencia con %d mediciones. Último valor %.2f", muestras.size(), ultimo.valor));
    }

    private int dp(float valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
