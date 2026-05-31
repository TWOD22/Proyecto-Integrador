package sigep;

import java.util.*;
import javax.swing.*;
import sigep.db.DB;

public class StatusModel {
    private static final Map<String, StatusModel> registry = new HashMap<>();
    private final String cedula;
    private final List<Runnable> listeners = new ArrayList<>();
    // 0 = Pendiente por documentos, 1 = Esperando asignacion, 2 = Listo
    private int estado = 0;

    private StatusModel(String cedula){ this.cedula = cedula; refreshFromDB(); }

    public static synchronized StatusModel get(String cedula){
        if (cedula == null) cedula = "";
        var m = registry.get(cedula);
        if (m == null) {
            m = new StatusModel(cedula);
            registry.put(cedula,m);
        }
        return m;
    }

    public synchronized void addListener(Runnable r){ listeners.add(r); }
    public synchronized void removeListener(Runnable r){ listeners.remove(r); }
    private synchronized void notifyListeners(){ for(var r: listeners) try{ r.run(); } catch(Exception ignored){} }

    public synchronized int getEstado(){ return estado; }

    public synchronized void setEstado(int s){
        if (s == this.estado) return;
        this.estado = s;
        notifyListeners();
    }

    public void refreshFromDB(){
        try {
            // If the student has an active assignment, prefer an "realizando" state (3).
            try {
                var asigs = DB.asignacionesPorEstudiante(cedula);
                if (asigs != null && !asigs.isEmpty()) { setEstado(3); return; }
            } catch (Exception ignored) {}
            var u = DB.buscarUsuario(cedula);
            int val = u != null ? u.habilitadoAsig : 0;
            setEstado(val);
        } catch (Exception ignored) {}
    }

    // Called by DB listeners to refresh models when data changes
    public static void refreshRegistryFor(String topic){
        if ("documentos".equals(topic) || "usuarios".equals(topic) || "asignaciones".equals(topic)){
            synchronized(registry){
                for(var m: registry.values()) m.refreshFromDB();
            }
        }
    }
}
