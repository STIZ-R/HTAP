package com.htap.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilitaires pour la journalisation (logging) avec SLF4J.
 *
 * Fournit une méthode centralisée pour obtenir un Logger,
 * ce qui évite de répéter la même ligne de code dans chaque classe.
 */
public class LogUtils {

    /**
     * Retourne un logger SLF4J pour la classe donnée.
     *
     * @param clazz classe pour laquelle le logger doit être créé
     * @return instance de Logger associée à la classe
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
