package dev.onefatir.triocrud;

import java.math.BigDecimal;
import java.math.RoundingMode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "aritmatika")
public class Aritmatika {

    // numeric(38,10): maksimal 28 digit bulat dan 10 digit desimal
    public static final int PRECISION = 38;
    public static final int SCALE = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = PRECISION, scale = SCALE)
    private BigDecimal bilangan1;

    // Salah satu dari: + - * /
    @Column(nullable = false, length = 1)
    private String operator;

    @Column(nullable = false, precision = PRECISION, scale = SCALE)
    private BigDecimal bilangan2;

    // Buang nol di belakang koma dari database (5.0000000000 -> 5)
    @PostLoad
    protected void onLoad() {
        bilangan1 = rapikan(bilangan1);
        bilangan2 = rapikan(bilangan2);
    }

    // Result tidak disimpan di database, selalu dihitung ulang dari bilangan1, operator, bilangan2
    @Transient
    public BigDecimal getResult() {
        return hitung(bilangan1, operator, bilangan2);
    }

    public static BigDecimal hitung(BigDecimal a, String operator, BigDecimal b) {
        BigDecimal hasil = switch (operator == null ? "" : operator) {
            case "+" -> a.add(b);
            case "-" -> a.subtract(b);
            case "*" -> a.multiply(b);
            case "/" -> {
                if (b.signum() == 0) {
                    throw new ArithmeticException("Tidak bisa membagi dengan nol");
                }
                yield a.divide(b, SCALE, RoundingMode.HALF_UP);
            }
            default -> throw new IllegalArgumentException("Operator tidak valid");
        };
        return rapikan(hasil);
    }

    private static BigDecimal rapikan(BigDecimal angka) {
        return angka == null ? null : new BigDecimal(angka.stripTrailingZeros().toPlainString());
    }

    // --- Getter dan Setter ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public BigDecimal getBilangan1() { return bilangan1; }
    public void setBilangan1(BigDecimal bilangan1) { this.bilangan1 = bilangan1; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    public BigDecimal getBilangan2() { return bilangan2; }
    public void setBilangan2(BigDecimal bilangan2) { this.bilangan2 = bilangan2; }
}
