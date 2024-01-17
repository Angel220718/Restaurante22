package com.example.reservacionrestaurant.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.reservacionrestaurant.ListaRestaurantActivity;
import com.example.reservacionrestaurant.R;
import com.example.reservacionrestaurant.ReservasDetalleActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ReservaFragment extends Fragment {


    private static final String TAG = "ReservaFragment";
    private EditText etNombreCliente;
    private Spinner spinnerMesas;
    private String restauranteId;
    private Button btnGuardarReserva;

    private String nombreCliente;

    private static final String ARG_MESAS_SELECCIONADAS = "mesas_seleccionadas";

    public ReservaFragment() {
        // Required empty public constructor
    }

    public static ReservaFragment newInstance(ArrayList<Integer> mesasSeleccionadas, String restauranteId) {
        ReservaFragment fragment = new ReservaFragment();
        Bundle args = new Bundle();
        args.putIntegerArrayList(ARG_MESAS_SELECCIONADAS, mesasSeleccionadas);
        args.putString("restauranteId", restauranteId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setNombreCliente(String nombre) {
        this.nombreCliente = nombre;
        etNombreCliente.setText(nombre);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reserva, container, false);

        etNombreCliente = view.findViewById(R.id.etNombreCliente);
        spinnerMesas = view.findViewById(R.id.spinnerMesas);
        btnGuardarReserva = view.findViewById(R.id.btnGuardarReserva);
        restauranteId = getArguments().getString("restauranteId");



        // Configurar el adaptador para el Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(), R.array.mesas_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMesas.setAdapter(adapter);

        // Agregar un escucha al Spinner para manejar la selección
        spinnerMesas.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                int numeroMesa = position + 1;  // Suponiendo que la posición en el Spinner representa el número de la mesa
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // Aquí puedes manejar el caso en que no se selecciona nada
            }
        });

        btnGuardarReserva.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nombreCliente = etNombreCliente.getText().toString().trim();
                ArrayList<Integer> mesasSeleccionadas = getArguments().getIntegerArrayList(ARG_MESAS_SELECCIONADAS);

                if (mesasSeleccionadas.isEmpty()) {
                    Toast.makeText(requireContext(), "Seleccione al menos una mesa.", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (Integer numeroMesa : mesasSeleccionadas) {
                    String numeroMesaFormateado = String.format(Locale.getDefault(), "%03d", numeroMesa);
                    DocumentReference mesaDocument = FirebaseFirestore.getInstance().collection(restauranteId + "_mesas").document("mesa_" + numeroMesaFormateado);

                    mesaDocument.get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            String estadoMesa = task.getResult().getString("estadoMesa");

                            if ("Ocupado".equals(estadoMesa)) {
                                Toast.makeText(requireContext(), "La mesa " + numeroMesa + " está ocupada. Seleccione otra mesa.", Toast.LENGTH_SHORT).show();
                            } else {
                                guardarReserva(restauranteId, nombreCliente, mesasSeleccionadas);
                            }
                        } else {
                            Log.e(TAG, "Error al obtener el estado de la mesa para mesa_" + numeroMesa, task.getException());
                        }
                    });
                }
            }
        });

        return view;
    }

    private void guardarReserva(String restauranteId, String nombreCliente, ArrayList<Integer> mesasSeleccionadas) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (mesasSeleccionadas.isEmpty()) {
            Toast.makeText(requireContext(), "Seleccione al menos una mesa.", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder mesasOcupadasMensaje = new StringBuilder();

        // Obtener el nombre del restaurante (puedes cambiar esto según cómo estén organizados tus datos)
        String nombreRestaurante = obtenerNombreRestaurante(restauranteId);

        for (Integer numeroMesa : mesasSeleccionadas) {
            String numeroMesaFormateado = String.format(Locale.getDefault(), "%03d", numeroMesa);
            DocumentReference mesaDocument = db.collection(restauranteId + "_mesas").document("mesa_" + numeroMesaFormateado);

            mesaDocument.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String estadoMesa = task.getResult().getString("estadoMesa");

                    if ("Ocupado".equals(estadoMesa)) {
                        mesasOcupadasMensaje.append("mesa ").append(numeroMesa).append(", ");
                    }

                    if (mesasSeleccionadas.indexOf(numeroMesa) == mesasSeleccionadas.size() - 1) {
                        if (mesasOcupadasMensaje.length() > 0) {
                            mesasOcupadasMensaje.deleteCharAt(mesasOcupadasMensaje.length() - 2);
                            Toast.makeText(requireContext(), "La(s) " + mesasOcupadasMensaje + " están ocupadas. Seleccione otras mesas.", Toast.LENGTH_SHORT).show();
                        } else {
                            StringBuilder nombreReserva = new StringBuilder("Reserva para:  ");

                            for (Integer mesa : mesasSeleccionadas) {
                                nombreReserva.append("mesa_").append(mesa).append(", ");
                            }

                            nombreReserva.deleteCharAt(nombreReserva.length() - 2);

                            String reservaId = nombreReserva.toString().replace(" ", "_");

                            Reserva reserva = new Reserva(nombreCliente);
                            reserva.setMesasSeleccionadas(mesasSeleccionadas, "Ocupado");
                            reserva.setNombreRestaurante(restauranteId);
                            reserva.setFechaReserva(obtenerFechaActual());  // Puedes utilizar un método para obtener la fecha actual
                            reserva.setHoraReserva(obtenerHoraActual());


                            db.collection(restauranteId + "_reservas").document(reservaId)
                                    .set(reserva)
                                    .addOnSuccessListener(documentReference -> {
                                        actualizarEstadoMesas(restauranteId, mesasSeleccionadas, "Ocupado");

                                        Toast.makeText(requireContext(), nombreReserva + " guardada con éxito", Toast.LENGTH_SHORT).show();

                                        startActivity(new Intent(getActivity(), ListaRestaurantActivity.class));

                                        Intent intent = new Intent(getActivity(), ReservasDetalleActivity.class);
                                        intent.putExtra("nombreReserva", nombreReserva.toString());
                                        intent.putExtra("mesasSeleccionadas", mesasSeleccionadas);
                                        intent.putExtra("nombreCliente", nombreCliente);
                                        intent.putExtra("fechaReserva", reserva.getFechaReserva());
                                        intent.putExtra("horaReserva", reserva.getHoraReserva());
                                        startActivity(intent);

                                        getActivity().getSupportFragmentManager().beginTransaction().remove(ReservaFragment.this).commit();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(requireContext(), "Error al guardar la reserva", Toast.LENGTH_SHORT).show();
                                        e.printStackTrace();
                                    });
                        }
                    }
                } else {
                    Log.e(TAG, "Error al obtener el estado de la mesa para mesa_" + numeroMesa, task.getException());
                }
            });
        }
    }

    // Método para obtener el nombre del restaurante según el ID (puedes personalizar esto según tu estructura de datos)
    private String obtenerNombreRestaurante(String restauranteId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Suponiendo que tienes una colección llamada "restaurantes"
        DocumentReference restauranteDocument = db.collection(restauranteId).document(restauranteId);

        restauranteDocument.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String nombreRestaurante = task.getResult().getString("nombreRestaurante");
                // Aquí puedes usar el nombre del restaurante según tus necesidades
                Log.d(TAG, "Nombre del restaurante obtenido: " + nombreRestaurante);
            } else {
                Log.e(TAG, "Error al obtener el nombre del restaurante para " + restauranteId, task.getException());
            }
        });

        // Puedes devolver un valor predeterminado o null en caso de error
        return "Nombre del Restaurante Predeterminado";
    }


    // Método para obtener la fecha actual (puedes personalizar esto según tus necesidades)
    private String obtenerFechaActual() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = new Date();
        return dateFormat.format(date);
    }

    private String obtenerHoraActual() {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        Date date = new Date();
        return timeFormat.format(date);
    }


    private void actualizarEstadoMesas(String restauranteId, ArrayList<Integer> mesasSeleccionadas, String estado) {
        if (mesasSeleccionadas == null || mesasSeleccionadas.isEmpty()) {
            Log.w(TAG, "Lista de mesas seleccionadas es nula o vacía");
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (Integer numeroMesa : mesasSeleccionadas) {
            String numeroMesaFormateado = String.format(Locale.getDefault(), "%03d", numeroMesa);
            DocumentReference mesaDocument = db.collection(restauranteId + "_mesas").document("mesa_" + numeroMesaFormateado);

            mesaDocument.update("estadoMesa", estado)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Estado de la mesa actualizado correctamente para mesa_" + numeroMesa);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error al actualizar el estado de la mesa para mesa_" + numeroMesa, e);
                    });
        }
    }
}