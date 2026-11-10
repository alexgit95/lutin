package com.alexgit95.lutin.transfert;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.metamodel.EntityType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;

/** Une colonne ajoutée à une entité sans mise à jour des descripteurs (et de la version) ferait oublier des données à l'export. */
@SpringBootTest
@ActiveProfiles("test")
class TablesConformiteTest {

    @Autowired EntityManagerFactory fabrique;

    private static Set<String> colonnesDeclarees(Class<?> entite) {
        Set<String> colonnes = new TreeSet<>();
        for (Field champ : entite.getDeclaredFields()) {
            if (Modifier.isStatic(champ.getModifiers()) || champ.isAnnotationPresent(Transient.class)
                    || champ.isAnnotationPresent(ManyToMany.class)) {
                continue;
            }
            JoinColumn jointure = champ.getAnnotation(JoinColumn.class);
            Column colonne = champ.getAnnotation(Column.class);
            if (jointure != null) {
                colonnes.add(jointure.name());
            } else if (colonne != null && !colonne.name().isEmpty()) {
                colonnes.add(colonne.name());
            } else {
                colonnes.add(champ.getName());
            }
        }
        return colonnes;
    }

    private static Set<String> colonnesDuDescripteur(TableTransfert table) {
        Set<String> noms = new TreeSet<>();
        table.colonnes().forEach(c -> noms.add(c.nom()));
        return noms;
    }

    @Test
    void chaqueEntiteADesDescripteursQuiCouvrentToutesSesColonnes() {
        Set<String> tablesVues = new HashSet<>();
        for (EntityType<?> type : fabrique.getMetamodel().getEntities()) {
            Class<?> classe = type.getJavaType();
            assertThat(classe.isAnnotationPresent(Entity.class)).isTrue();
            String nom = classe.getAnnotation(Table.class).name();
            TableTransfert table = Tables.parNom(nom);

            assertThat(table).as("table " + nom + " absente des descripteurs").isNotNull();
            assertThat(colonnesDuDescripteur(table)).as("colonnes de " + nom).isEqualTo(colonnesDeclarees(classe));
            tablesVues.add(nom);
        }
        assertThat(tablesVues).hasSize(Tables.TOUTES.size() - 1);
    }

    @Test
    void lesTablesDeJointureSontDecritesAvecLeurs() throws Exception {
        Set<String> jointures = new HashSet<>();
        for (EntityType<?> type : fabrique.getMetamodel().getEntities()) {
            for (Field champ : type.getJavaType().getDeclaredFields()) {
                JoinTable jointure = champ.getAnnotation(JoinTable.class);
                if (jointure != null) {
                    jointures.add(jointure.name());
                    TableTransfert table = Tables.parNom(jointure.name());
                    assertThat(table).as("jointure " + jointure.name()).isNotNull();
                    assertThat(colonnesDuDescripteur(table)).containsExactlyInAnyOrder(
                            jointure.joinColumns()[0].name(), jointure.inverseJoinColumns()[0].name());
                }
            }
        }
        assertThat(jointures).containsExactly("cadeau_tag");
    }

    @Test
    void lOrdreDesTablesRespecteLesReferences() {
        List<String> vues = new java.util.ArrayList<>();
        for (TableTransfert table : Tables.TOUTES) {
            for (Colonne colonne : table.colonnes()) {
                if (colonne.parent() != null) {
                    assertThat(vues).as(table.nom() + "." + colonne.nom()).contains(colonne.parent());
                }
            }
            vues.add(table.nom());
        }
        assertThat(Tables.VERSION).isPositive();
    }
}
