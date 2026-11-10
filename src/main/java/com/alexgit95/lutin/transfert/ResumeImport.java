package com.alexgit95.lutin.transfert;

import java.util.Map;

/** Nombre de lignes importées par table, dans l'ordre des tables. */
public record ResumeImport(Map<String, Long> lignesParTable) {

    public long total() {
        return lignesParTable.values().stream().mapToLong(Long::longValue).sum();
    }
}
