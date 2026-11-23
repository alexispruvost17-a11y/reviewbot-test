package fr.uge.loganalyzer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PmdTest {

    private static final int MAGIC_NUMBER = 42;

    private String name;
    public int publicCounter = 0;

    public PmdTest() {
        name = "Test";
    } 

    // 1. Variable inutilisée et nombre magique
    public int unusedVariable() {
        int unused = 42;
        int anotherUnused = 100;
        return MAGIC_NUMBER;
    }

    // 2. Comparaison de chaînes avec ==
    public boolean compareStrings(String first, String second) {
        return first == second;
    }

    // 3. Condition inutile
    public boolean unnecessaryCondition(boolean value) {
        if (value == true) {
            return true;
        } else {
            return false;
        }
    }

    // 4. Boucle qui pourrait être simplifiée
    public void uselessLoop() {
        for (int i = 0; i < 10; i++) {
            System.out.println("Test");
        }
    }

    // 5. Catch vide
    public void emptyCatch() {
        try {
            int result = 10 / 0;
            System.out.println(result);
        } catch (Exception exception) {
        }
    }

    // 6. Exception trop générale
    public void broadException() throws Exception {
        throw new Exception("Erreur");
    }

    // 7. Paramètre inutilisé
    public void unusedParameter(String message) {
        System.out.println("Hello");
    }

    // 8. Méthode trop complexe
    public String complexMethod(int value) {
        if (value > 0) {
            if (value < 10) {
                if (value != 5) {
                    if (value % 2 == 0) {
                        return "Positif pair";
                    } else {
                        return "Positif impair";
                    }
                } else {
                    return "Cinq";
                }
            } else {
                return "Grand";
            }
        } else if (value < 0) {
            return "Négatif";
        } else {
            return "Zéro";
        }
    }

    // 9. Utilisation d'une collection brute
    public void rawCollection() {
        List values = new ArrayList();
        values.add("Bonjour");
        values.add(42);
        System.out.println(values);
    }

    // 10. Objet inutile
    public String uselessStringOperation() {
        String message = new String("Bonjour");
        return message;
    }

    // 11. Retour inutilement compliqué
    public boolean alwaysTrue() {
        return true == true;
    }

    // 12. Attribut public modifiable
    public void incrementCounter() {
        publicCounter = publicCounter + 1;
    }

    // 13. Fichier non fermé
    public void openFile() {
        try {
            File file = new File("test.txt");
            System.out.println(file.length());
        } catch (Exception exception) {
            System.out.println("Erreur");
        }
    }

    // 14. Code mort
    public int deadCode() {
        return 10;
        // System.out.println("Ce code ne sera jamais exécuté");
    }

    // 15. Variable locale redondante
    public int redundantVariable(int value) {
        int result = value;
        return result;
    }

    // 16. Méthode statique possible
    public int calculate(int first, int second) {
        return first + second;
    }

    // 17. Test inutile
    public void redundantBooleanTest(boolean value) {
        if (value == false) {
            System.out.println("Faux");
        }
    }

    // 18. Champ jamais utilisé
    private int unusedField = 99;

    // 19. Méthode vide
    public void emptyMethod() {
    }

    // 20. Affectation redondante
    public void redundantAssignment() {
        int value = 10;
        value = 20;
        System.out.println(value);
    }
}
