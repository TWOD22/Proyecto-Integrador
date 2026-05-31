package tools;

import sigep.db.DB;

public class SyncEstados {
    public static void main(String[] args) {
        System.out.println("Iniciando sincronización de estados de asignación...");
        int n = DB.syncAsignacionesToUsuarios();
        System.out.println("Usuarios actualizados: " + n);
        System.out.println("Hecho.");
    }
}
