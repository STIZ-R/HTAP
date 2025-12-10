package com.htap.meta;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Utilitaires pour la gestion des threads.
 *
 * Fournit des méthodes d'aide pour créer des pools de threads
 * utilisés par le proxy pour exécuter des tâches en parallèle.
 */
public class ThreadUtils {

    /**
     * Crée un pool de threads fixe avec un nombre donné de threads.
     *
     * @param nThreads nombre de threads dans le pool
     * @return un ExecutorService configuré avec un pool fixe
     */
    public static ExecutorService createFixedThreadPool(int nThreads) {
        return Executors.newFixedThreadPool(nThreads);
    }
}
