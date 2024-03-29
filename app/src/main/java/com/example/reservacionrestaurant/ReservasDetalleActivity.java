package com.example.reservacionrestaurant;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ReservasDetalleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservas_detalle);

        TextView tvNombreRestaurante = findViewById(R.id.tvNombreRestaurante);
        TextView tvNombreCliente = findViewById(R.id.tvNombreCliente);
        TextView tvFechaReserva = findViewById(R.id.tvFechaReserva);
        TextView tvMesasReservadas = findViewById(R.id.tvMesasReservadas);
        TextView tvHoraReserva = findViewById(R.id.tvHoraReserva);

        Intent intent = getIntent();
        String nombreReserva = intent.getStringExtra("nombreReserva");
        ArrayList<Integer> mesasSeleccionadas = intent.getIntegerArrayListExtra("mesasSeleccionadas");
        String nombreCliente = intent.getStringExtra("nombreCliente");
        String fechadeReserva = intent.getStringExtra("fechadeReserva");
        String horadeReserva = intent.getStringExtra("horadeReserva");

        tvNombreRestaurante.setText(nombreReserva);
        tvNombreCliente.setText("Nombre del Cliente: " + nombreCliente);


        if (mesasSeleccionadas != null) {
            StringBuilder mesasReservadas = new StringBuilder("Mesas Reservadas: ");
            for (Integer mesa : mesasSeleccionadas) {
                mesasReservadas.append("mesa ").append(mesa).append(", ");
            }
            mesasReservadas.deleteCharAt(mesasReservadas.length() - 2);
            tvMesasReservadas.setText(mesasReservadas.toString());
        } else {
            tvMesasReservadas.setText("Mesas Reservadas: (sin mesas seleccionadas)");
        }
        tvFechaReserva.setText("Fecha de Reserva: " + fechadeReserva);
        tvHoraReserva.setText("Hora de Reserva: " + horadeReserva);
    }
}