package dev.mobrealms.core;

/** A walking goal must improve its distance and has an absolute lifetime. */
public final class WorkProgress {
    private final long started;
    private long improved;
    private double best;
    public WorkProgress(long now,double distance){started=improved=now;best=distance;}
    public boolean expired(long now,double distance){
        if(distance<best-.5){best=distance;improved=now;}
        return now-improved>=200||now-started>=1200;
    }
}
