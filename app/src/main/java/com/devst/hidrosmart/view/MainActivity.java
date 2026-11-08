package com.devst.hidrosmart.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.modelo.ComentarioUsuario;
import com.devst.hidrosmart.viewmodel.HuertoViewModel;
import com.google.android.material.tabs.TabLayout;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_MODO_DEMO = "modo_demo";
    private static final String STATE_FRAGMENT_ACTIVO = "fragment_activo";

    private HuertoViewModel viewModel;
    private TabLayout tabNavegacionAdmin;
    private Button btnAgregarTorre, btnEliminarTorre, btnCerrarSesionTop;
    private ImageButton btnAjustesTop;
    private TextView tvTituloToolbar;

    private Fragment fragmentInicio;
    private Fragment fragmentControl;
    private Fragment fragmentHistorial;
    private Fragment fragmentActivo;

    private boolean esModoDemo = false;
    private String rol = Login.ROL_ADMIN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        viewModel = new ViewModelProvider(this).get(HuertoViewModel.class);

        rol = getIntent().getStringExtra(Login.EXTRA_ROL);
        if (rol == null) rol = Login.ROL_ADMIN;
        esModoDemo = getIntent().getBooleanExtra(EXTRA_MODO_DEMO, false);

        inicializarComponentes();
        configurarFragmentosIniciales(savedInstanceState);
        configurarNavegacionPorRol();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (fragmentActivo != null) {
            outState.putString(STATE_FRAGMENT_ACTIVO, fragmentActivo.getTag());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null) {
            viewModel.reanudarSimulacion();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (viewModel != null) {
            viewModel.pausarSimulacion();
        }
    }

    private void inicializarComponentes() {
        tvTituloToolbar = findViewById(R.id.tvTituloToolbar);
        tabNavegacionAdmin = findViewById(R.id.tabNavegacionAdmin);
        btnAgregarTorre = findViewById(R.id.btnAgregarTorre);
        btnEliminarTorre = findViewById(R.id.btnEliminarTorre);
        btnCerrarSesionTop = findViewById(R.id.btnCerrarSesionTop);
        btnAjustesTop = findViewById(R.id.btnAjustesTop);

        btnCerrarSesionTop.setOnClickListener(v -> cerrarSesion());
        btnAjustesTop.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, AjustesActivity.class)));

        btnAgregarTorre.setOnClickListener(v -> {
            if (!viewModel.agregarTorre()) {
                Toast.makeText(this, R.string.mensaje_maximo_torres, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.mensaje_torre_agregada, Toast.LENGTH_SHORT).show();
            }
        });

        btnEliminarTorre.setOnClickListener(v -> {
            if (!viewModel.eliminarTorre()) {
                Toast.makeText(this, R.string.mensaje_minimo_torres, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.mensaje_torre_eliminada, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarFragmentosIniciales(Bundle savedInstanceState) {
        FragmentManager fm = getSupportFragmentManager();
        if (Login.ROL_USER.equals(rol)) {
            fragmentInicio = fm.findFragmentByTag("inicio");
            fragmentHistorial = fm.findFragmentByTag("historial");

            if (fragmentInicio == null || fragmentHistorial == null) {
                androidx.fragment.app.FragmentTransaction transaction = fm.beginTransaction();
                if (fragmentInicio == null) {
                    fragmentInicio = new InicioFragment();
                    transaction.add(R.id.contenedorFragment, fragmentInicio, "inicio");
                }
                if (fragmentHistorial == null) {
                    fragmentHistorial = new HistorialFragment();
                    transaction.add(R.id.contenedorFragment, fragmentHistorial, "historial").hide(fragmentHistorial);
                }
                transaction.commitNow();
            }

            String tagActivo = savedInstanceState == null
                    ? "inicio"
                    : savedInstanceState.getString(STATE_FRAGMENT_ACTIVO, "inicio");
            fragmentActivo = "historial".equals(tagActivo) ? fragmentHistorial : fragmentInicio;
            androidx.fragment.app.FragmentTransaction visibility = fm.beginTransaction();
            if (fragmentActivo == fragmentInicio) visibility.show(fragmentInicio);
            else visibility.hide(fragmentInicio);
            if (fragmentActivo == fragmentHistorial) visibility.show(fragmentHistorial);
            else visibility.hide(fragmentHistorial);
            visibility.commitNow();
        } else {
            fragmentInicio = fm.findFragmentByTag("inicio");
            fragmentControl = fm.findFragmentByTag("control");

            boolean necesitaAgregar = fragmentInicio == null || fragmentControl == null;
            if (necesitaAgregar) {
                androidx.fragment.app.FragmentTransaction transaction = fm.beginTransaction();
                if (fragmentInicio == null) {
                    fragmentInicio = new InicioFragment();
                    transaction.add(R.id.contenedorFragment, fragmentInicio, "inicio");
                }
                if (fragmentControl == null) {
                    fragmentControl = new ControlFragment();
                    transaction.add(R.id.contenedorFragment, fragmentControl, "control").hide(fragmentControl);
                }
                transaction.commitNow();
            }

            String tagActivo = savedInstanceState == null
                    ? "inicio"
                    : savedInstanceState.getString(STATE_FRAGMENT_ACTIVO, "inicio");
            if (!"control".equals(tagActivo) || fragmentControl == null) {
                tagActivo = "inicio";
            }
            fragmentActivo = "control".equals(tagActivo) ? fragmentControl : fragmentInicio;

            androidx.fragment.app.FragmentTransaction visibility = fm.beginTransaction();
            if (fragmentControl != null) {
                if (fragmentActivo == fragmentControl) visibility.show(fragmentControl);
                else visibility.hide(fragmentControl);
            }
            if (fragmentActivo == fragmentInicio) visibility.show(fragmentInicio);
            else visibility.hide(fragmentInicio);
            visibility.commitNow();
        }
    }

    private void cambiarFragment(Fragment nuevoFragment) {
        if (fragmentActivo == nuevoFragment) return;
        getSupportFragmentManager().beginTransaction()
                .hide(fragmentActivo)
                .show(nuevoFragment)
                .commit();
        fragmentActivo = nuevoFragment;
    }

    private void configurarNavegacionPorRol() {
        if (Login.ROL_USER.equals(rol)) {
            tvTituloToolbar.setText(R.string.titulo_usuario);
            btnAgregarTorre.setVisibility(View.GONE);
            btnEliminarTorre.setVisibility(View.GONE);
            if (tabNavegacionAdmin.getTabAt(0) != null) {
                tabNavegacionAdmin.getTabAt(0).setText(R.string.tab_estado);
            }
            if (tabNavegacionAdmin.getTabAt(1) != null) {
                tabNavegacionAdmin.getTabAt(1).setText(R.string.tab_historial);
            }
            TabLayout.Tab tabUsuarios = tabNavegacionAdmin.getTabAt(2);
            if (tabUsuarios != null) tabNavegacionAdmin.removeTab(tabUsuarios);
        } else if (esModoDemo) {
            tvTituloToolbar.setText(R.string.titulo_demo);
            btnAgregarTorre.setVisibility(View.GONE);
            btnEliminarTorre.setVisibility(View.GONE);
            TabLayout.Tab tabUsuarios = tabNavegacionAdmin.getTabAt(2);
            if (tabUsuarios != null) tabNavegacionAdmin.removeTab(tabUsuarios);
        } else {
            tvTituloToolbar.setText(R.string.titulo_admin);
        }

        tabNavegacionAdmin.setVisibility(View.VISIBLE);

        tabNavegacionAdmin.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) {
                    cambiarFragment(fragmentInicio);
                } else if (Login.ROL_USER.equals(rol) && pos == 1) {
                    cambiarFragment(fragmentHistorial);
                } else if (pos == 1) {
                    cambiarFragment(fragmentControl);
                } else if (!esModoDemo && pos == 2) {
                    mostrarDialogoUsuariosRegistrados();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (!Login.ROL_USER.equals(rol) && !esModoDemo && tab.getPosition() == 2) {
                    mostrarDialogoUsuariosRegistrados();
                }
            }
        });

        int tabInicial = fragmentActivo == fragmentControl || fragmentActivo == fragmentHistorial ? 1 : 0;
        TabLayout.Tab tab = tabNavegacionAdmin.getTabAt(tabInicial);
        if (tab != null && !tab.isSelected()) tab.select();
    }

    private void mostrarDialogoUsuariosRegistrados() {
        List<String> usuarios = viewModel.getUsuariosGenerales().getValue();
        if (usuarios == null || usuarios.isEmpty()) {
            Toast.makeText(this, R.string.mensaje_sin_usuarios, Toast.LENGTH_SHORT).show();
            return;
        }

        String[] listaArray = usuarios.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_usuarios_titulo)
                .setItems(listaArray, (dialog, which) -> {
                    String seleccionado = listaArray[which];
                    mostrarObservacionesUsuario(seleccionado);
                })
                .setPositiveButton(R.string.accion_cerrar, (d, w) -> {
                    TabLayout.Tab tab0 = tabNavegacionAdmin.getTabAt(0);
                    if (tab0 != null) tab0.select();
                })
                .show();
    }

    private void mostrarObservacionesUsuario(String usuario) {
        List<ComentarioUsuario> comentarios = viewModel.getComentarios().getValue();
        StringBuilder sb = new StringBuilder();

        if (comentarios != null) {
            for (ComentarioUsuario c : comentarios) {
                if (c.usuario.equals(usuario)) {
                    sb.append(getString(R.string.formato_observacion_usuario,
                            c.fechaHora, c.torre, c.mensaje));
                }
            }
        }

        if (sb.length() == 0) {
            sb.append(getString(R.string.dialog_usuario_sin_observaciones));
        }

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.dialog_usuario_observaciones_titulo, usuario))
                .setMessage(sb.toString().trim())
                .setPositiveButton(R.string.accion_volver, (d, w) -> mostrarDialogoUsuariosRegistrados())
                .show();
    }

    private void cerrarSesion() {
        Intent intent = new Intent(MainActivity.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
