package com.example.reservacionrestaurant.fragments;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.example.reservacionrestaurant.ListaRestaurantActivity;
import com.example.reservacionrestaurant.R;
import com.example.reservacionrestaurant.ReservasDetalleActivity;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReservaFragment extends Fragment {
    private static final String TAG = "ReservaFragment";
    private EditText etNombreCliente;
    private String restauranteId;
    private Button btnGuardarReserva;

    private EditText etFechaReserva;
    private EditText etHoraReserva;

    private static final String ARG_MESAS_SELECCIONADAS = "mesas_seleccionadas";

    public ReservaFragment() {
    }

    public static ReservaFragment newInstance(ArrayList<Integer> mesasSeleccionadas, String restauranteId) {
        ReservaFragment fragment = new ReservaFragment();
        Bundle args = new Bundle();
        args.putIntegerArrayList(ARG_MESAS_SELECCIONADAS, mesasSeleccionadas);
        args.putString("restauranteId", restauranteId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reserva, container, false);

        etNombreCliente = view.findViewById(R.id.etNombreCliente);
        btnGuardarReserva = view.findViewById(R.id.btnGuardarReserva);
        restauranteId = getArguments().getString("restauranteId");
        etFechaReserva = view.findViewById(R.id.etFechaReserva);
        etHoraReserva = view.findViewById(R.id.etHoraReserva);

        // Verificar si los EditText se han inicializado correctamente
        if (etFechaReserva == null || etHoraReserva == null) {
            Log.e(TAG, "Error: EditText no se ha inicializado correctamente.");
            return view;
        }

        // ...

        etFechaReserva.setOnClickListener(v -> mostrarDatePicker());
        etHoraReserva.setOnClickListener(v -> mostrarTimePicker());

        btnGuardarReserva.setOnClickListener(v -> {
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
        });

        return view;
    }

    private String mostrarDatePicker() {
        // Obtener la fecha actual
        final Calendar c = Calendar.getInstance();
        int añoActual = c.get(Calendar.YEAR);
        int mesActual = c.get(Calendar.MONTH);
        int diaActual = c.get(Calendar.DAY_OF_MONTH);

        // Crear un nuevo DatePickerDialog y configurarlo según tus necesidades
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, year, monthOfYear, dayOfMonth) -> {
                    // Procesar la fecha seleccionada
                    String fechaSeleccionada = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, monthOfYear + 1, dayOfMonth);
                    etFechaReserva.setText(fechaSeleccionada);
                },
                añoActual, mesActual, diaActual);

        // Mostrar el diálogo
        datePickerDialog.show();

        // Devolver la fecha seleccionada
        return etFechaReserva.getText().toString();
    }

    private String mostrarTimePicker() {
        // Obtener la hora actual
        final Calendar c = Calendar.getInstance();
        int horaActual = c.get(Calendar.HOUR_OF_DAY);
        int minutoActual = c.get(Calendar.MINUTE);

        // Crear un nuevo TimePickerDialog y configurarlo según tus necesidades
        TimePickerDialog timePickerDialog = new TimePickerDialog(
                requireContext(),
                (view, hourOfDay, minute) -> {
                    // Procesar la hora seleccionada
                    String horaSeleccionada = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                    etHoraReserva.setText(horaSeleccionada);
                },
                horaActual, minutoActual, true);

        // Mostrar el diálogo
        timePickerDialog.show();

        // Devolver la hora seleccionada
        return etHoraReserva.getText().toString();
    }

    private void guardarReserva(String restauranteId, String nombreCliente, ArrayList<Integer> mesasSeleccionadas) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (mesasSeleccionadas.isEmpty()) {
            Toast.makeText(requireContext(), "Seleccione al menos una mesa.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verificar que la fecha seleccionada sea válida
        if (verificarFechaSeleccionada()) {
            StringBuilder nombreReserva = new StringBuilder("Reserva para: ");

            for (Integer mesa : mesasSeleccionadas) {
                nombreReserva.append("mesa_").append(mesa).append(", ");
            }

            nombreReserva.deleteCharAt(nombreReserva.length() - 2);

            String reservaId = nombreReserva.toString().replace(" ", "_");

            Reserva reserva = new Reserva(nombreCliente);
            reserva.setMesasSeleccionadas(mesasSeleccionadas, "Ocupado");
            reserva.setNombreRestaurante(restauranteId);

            // Obtener la fecha actual
            String fechaActual = obtenerFechaActual();

            // Obtener la fecha seleccionada
            String fechaReserva = etFechaReserva.getText().toString();

            // Añadir un día a la fecha actual
            String fechaActualMasUnDia = sumarDiasAFecha(fechaActual, 1);

            if (fechaReserva.compareTo(fechaActualMasUnDia) >= 0) {
                reserva.setFechaReserva(obtenerFechaActual());
                reserva.setHoraReserva(obtenerHoraActual());
                reserva.setFechadeReserva(fechaReserva);
                reserva.setHoradeReserva(mostrarTimePicker());


                db.collection(restauranteId + "_reservas").document(reservaId)
                        .set(reserva)
                        .addOnSuccessListener(documentReference -> {
                            // Actualizar el estado de las mesas
                            actualizarEstadoMesas(restauranteId, mesasSeleccionadas, "Ocupado");

                            Toast.makeText(requireContext(), nombreReserva + " guardada con éxito", Toast.LENGTH_SHORT).show();

                            // Navegar a la lista de restaurantes
                            startActivity(new Intent(getActivity(), ListaRestaurantActivity.class));

                            // Mostrar detalles de la reserva
                            Intent intent = new Intent(getActivity(), ReservasDetalleActivity.class);
                            intent.putExtra("nombreReserva", nombreReserva.toString());
                            intent.putExtra("mesasSeleccionadas", mesasSeleccionadas);
                            intent.putExtra("nombreCliente", nombreCliente);
                            intent.putExtra("fechaReserva", reserva.getFechaReserva());
                            intent.putExtra("horaReserva", reserva.getHoraReserva());
                            intent.putExtra("fechadeReserva", reserva.getFechadeReserva());
                            intent.putExtra("horadeReserva", reserva.getHoradeReserva());
                            startActivity(intent);


                            // Eliminar el fragmento actual
                            getActivity().getSupportFragmentManager().beginTransaction().remove(ReservaFragment.this).commit();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(requireContext(), "Error al guardar la reserva", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        });
            } else {
                Toast.makeText(requireContext(), "Seleccione una fecha válida para la reserva (al menos 1 día después de la fecha actual).", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(requireContext(), "Seleccione una fecha válida para la reserva.", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean verificarFechaSeleccionada() {
        String fechaReserva = etFechaReserva.getText().toString();

        // Verificar si la fecha está seleccionada
        return !fechaReserva.isEmpty();
    }

    private String sumarDiasAFecha(String fecha, int diasASumar) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();

        try {
            Date fechaDate = dateFormat.parse(fecha);
            calendar.setTime(fechaDate);
            calendar.add(Calendar.DAY_OF_MONTH, diasASumar);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return dateFormat.format(calendar.getTime());
    }

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
