package com.example.reservacionrestaurant;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.reservacionrestaurant.fragments.Reserva;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CuentaActivity extends AppCompatActivity implements ReservaListAdapter.OnReservaDeleteListener {

    private String restauranteId;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<Reserva> listaReservas = new ArrayList<>();
    private ListView listView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cuenta);

        restauranteId = getIntent().getStringExtra("restaurante_id");

        listView = findViewById(R.id.listViewReservas);

        cargarReservas();

    }

    private void cargarReservas() {
        CollectionReference reservasCollection = db.collection(restauranteId + "_reservas");

        reservasCollection.get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            listaReservas.clear(); // Limpiar la lista antes de cargar nuevas reservas

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String reservaId = document.getId();
                                Reserva reserva = document.toObject(Reserva.class);
                                reserva.setId(reservaId); // Establecer el ID en la reserva
                                listaReservas.add(reserva);
                            }

                            ReservaListAdapter adapter = new ReservaListAdapter(CuentaActivity.this, listaReservas);
                            adapter.setOnReservaDeleteListener(CuentaActivity.this);
                            listView.setAdapter(adapter);
                        } else {
                            Toast.makeText(CuentaActivity.this, "Error al obtener datos: " + task.getException(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }


    @Override
    public void onReservaDelete(Reserva reserva) {
        String reservaId = reserva.getId();

        eliminarReservaDeFirestore(reservaId);

        listaReservas.remove(reserva);
        ((ReservaListAdapter) listView.getAdapter()).notifyDataSetChanged();
    }

    private void eliminarReservaDeFirestore(String reservaId) {
        Log.d("CuentaActivity", "Intentando eliminar reserva con ID: " + reservaId);

        Reserva reserva = encontrarReservaPorId(reservaId);
        if (reserva != null) {
            ArrayList<Integer> mesasSeleccionadas = reserva.getMesasSeleccionadas();
            cambiarEstadoMesasALibre(restauranteId, mesasSeleccionadas, reserva.getEstadoMesa());

            CollectionReference reservasCollection = db.collection(restauranteId + "_reservas");

            reservasCollection.document(reservaId).delete()
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(CuentaActivity.this, "Error al eliminar reserva de Firestore: " + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }
    }

    //    private void cambiarEstadoMesasALibre(ArrayList<Integer> mesasSeleccionadas) {
//        // Lógica para cambiar el estado de las mesas a "Libre"
//        // Puedes utilizar la referencia a la colección de mesas y actualizar el estado de cada mesa
//
//        for (Integer mesa : mesasSeleccionadas) {
//            // Actualizar el estado de cada mesa
//            // Por ejemplo, supongamos que tienes una colección "mesas" con documentos identificados por su número
//            String numeroMesaFormateado = String.format(Locale.getDefault(), "%03d", mesa);
//            DocumentReference mesaDocument = FirebaseFirestore.getInstance().collection(restauranteId + "_mesas").document("mesa_" + numeroMesaFormateado);
//            mesasCollection.document(String.valueOf(mesa)).update("estadoMesa", "Libre");
//        }
//    }
    private void cambiarEstadoMesasALibre(String restauranteId, ArrayList<Integer> mesasSeleccionadas, String estado) {
        if (mesasSeleccionadas == null || mesasSeleccionadas.isEmpty()) {

            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        for (Integer numeroMesa : mesasSeleccionadas) {
            String numeroMesaFormateado = String.format(Locale.getDefault(), "%03d", numeroMesa);
            DocumentReference mesaDocument = db.collection(restauranteId + "_mesas").document("mesa_" + numeroMesaFormateado);

            mesaDocument.update("estadoMesa", "Libre")
                    .addOnSuccessListener(aVoid -> {

                    })
                    .addOnFailureListener(e -> {

                    });
        }
    }

    private Reserva encontrarReservaPorId(String reservaId) {
        for (Reserva reserva : listaReservas) {
            if (reserva.getId().equals(reservaId)) {
                return reserva;
            }
        }
        return null;
    }

}
