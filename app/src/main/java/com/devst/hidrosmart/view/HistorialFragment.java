package com.devst.hidrosmart.view;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.devst.hidrosmart.R;
import com.devst.hidrosmart.modelo.Huerto;
import com.devst.hidrosmart.viewmodel.HuertoViewModel;

import java.util.ArrayList;
import java.util.List;

public class HistorialFragment extends Fragment {

    private HuertoViewModel viewModel;

    private Spinner spnTorreComentario;
    private EditText etComentario;
    private Button btnEnviarComentario;
    private TextView tvUltimoComentario;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(HuertoViewModel.class);

        spnTorreComentario = view.findViewById(R.id.spnTorreComentario);
        etComentario = view.findViewById(R.id.etComentario);
        btnEnviarComentario = view.findViewById(R.id.btnEnviarComentario);
        tvUltimoComentario = view.findViewById(R.id.tvUltimoComentario);

        String usuarioActual = requireActivity().getIntent().getStringExtra(Login.EXTRA_USUARIO_ACTUAL);
        if (usuarioActual == null) usuarioActual = "user";
        final String remitente = usuarioActual;

        viewModel.getListaTorres().observe(getViewLifecycleOwner(), torres -> {
            if (torres == null) return;
            List<String> opciones = new ArrayList<>();
            for (Huerto h : torres) {
                opciones.add(String.format("Torre %d (%s)", h.idx, h.perfil.nombre));
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, opciones);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spnTorreComentario.setAdapter(adapter);
        });

        btnEnviarComentario.setOnClickListener(v -> {
            String texto = etComentario.getText().toString().trim();
            if (texto.isEmpty()) {
                Toast.makeText(requireContext(), "Escribe una observación antes de enviar", Toast.LENGTH_SHORT).show();
                return;
            }

            if (spnTorreComentario.getSelectedItem() == null) {
                Toast.makeText(requireContext(), "Selecciona una torre", Toast.LENGTH_SHORT).show();
                return;
            }

            String torreSeleccionada = spnTorreComentario.getSelectedItem().toString();
            viewModel.registrarComentario(remitente, torreSeleccionada, texto);

            tvUltimoComentario.setVisibility(View.VISIBLE);
            tvUltimoComentario.setText(String.format("✓ Observación guardada para %s:\n\"%s\"", torreSeleccionada, texto));
            etComentario.setText("");
            Toast.makeText(requireContext(), "Observación enviada al supervisor", Toast.LENGTH_SHORT).show();
        });
    }
}