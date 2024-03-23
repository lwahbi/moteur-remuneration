package ma.globalperformance.enums;

public enum BaseCalculType {
    PRINCIPAL,
    TRANSACTION,
    FACTURE,
    FRAIS;

    public static BaseCalculType fromString(String baseCalcul) {
        return BaseCalculType.valueOf(baseCalcul.toUpperCase());
    }
}