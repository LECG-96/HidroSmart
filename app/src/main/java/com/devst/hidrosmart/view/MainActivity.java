package com.devst.hidrosmart.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

    private HuertoViewModel viewModel;
    private TabLayout tabNavegacionAdmin;
    private Button btnAgregarTorre, btnEliminarTorre, btnCerrarSesionTop;
    private TextView tvTituloToolbar;

    private final Fragment fragmentInicio = new InicioFragment();
    private final Fragment fragmentControl = new ControlFragment();
    private final Fragment fragmentHistorial = new HistorialFragment();
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
        configurarFragmentosIniciales();
        configurarNavegacionPorRol();
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

        btnCerrarSesionTop.setOnClickListener(v -> cerrarSesion());

        btnAgregarTorre.setOnClickListener(v -> {
            if (!viewModel.agregarTorre()) {
                Toast.makeText(this, "Máximo 4 torres alcanzado", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Torre agregada", Toast.LENGTH_SHORT).show();
            }
        });

        btnEliminarTorre.setOnClickListener(v -> {
            if (!viewModel.eliminarTorre()) {
                Toast.makeText(this, "Debe quedar al menos 1 torre", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Torre eliminada", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void configurarFragmentosIniciales() {
        FragmentManager fm = getSupportFragmentManager();
        if (Login.ROL_USER.equals(rol)) {
            fm.beginTransaction()
                    .add(R.id.contenedorFragment, fragmentHistorial, "historial")
                    .commit();
            fragmentActivo = fragmentHistorial;
        } else {
            fm.beginTransaction()
                    .add(R.id.contenedorFragment, fragmentControl, "control").hide(fragmentControl)
                    .add(R.id.contenedorFragment, fragmentInicio, "inicio")
                    .commit();
            fragmentActivo = fragmentInicio;
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
            tabNavegacionAdmin.setVisibility(View.GONE);
            btnAgregarTorre.setVisibility(View.GONE);
            btnEliminarTorre.setVisibility(View.GONE);
            return;
        }

        if (esModoDemo) {
            tvTituloToolbar.setText(R.string.titulo_demo);
            if (tabNavegacionAdmin.getTabCount() >= 3) {
                tabNavegacionAdmin.removeTabAt(2);
            }
        } else {
            tvTituloToolbar.setText(R.string.titulo_admin);
        }

        tabNavegacionAdmin.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if (pos == 0) {
                    cambiarFragment(fragmentInicio);
                } else if (pos == 1) {
                    cambiarFragment(fragmentControl);
                } else if (pos == 2) {
                    mostrarDialogoUsuariosRegistrados();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 2) {
                    mostrarDialogoUsuariosRegistrados();
                }
            }
        });
    }

    private void mostrarDialogoUsuariosRegistrados() {
        List<String> usuarios = viewModel.getUsuariosGenerales().getValue();
        if (usuarios == null || usuarios.isEmpty()) {
            Toast.makeText(this, "No hay usuarios registrados", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] listaArray = usuarios.toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Usuarios Generales Registrados")
                .setItems(listaArray, (dialog, which) -> {
                    String seleccionado = listaArray[which];
                    mostrarObservacionesUsuario(seleccionado);
                })
                .setPositiveButton("Cerrar", (d, w) -> {
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
                    sb.append("• [").append(c.fechaHora).append("] ").append(c.torre).append(":\n")
                            .append("\"").append(c.mensaje).append("\"\n\n");
                }
            }
        }

        if (sb.length() == 0) {
            sb.append("Este usuario aún no registra observaciones.");
        }

        new AlertDialog.Builder(this)
                .setTitle("Observaciones de: " + usuario)
                .setMessage(sb.toString().trim())
                .setPositiveButton("Volver", (d, w) -> mostrarDialogoUsuariosRegistrados())
                .show();
    }

    private void cerrarSesion() {
        Intent intent = new Intent(MainActivity.this, Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}