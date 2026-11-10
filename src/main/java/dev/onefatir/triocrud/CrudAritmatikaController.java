package dev.onefatir.triocrud;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CrudAritmatikaController {

    @Autowired
    private AritmatikaRepository aritmatikaRepository;

    @GetMapping("/")
    public String beranda() {
        return "redirect:/crud-aritmatika";
    }

    // Menampilkan halaman utama (Form dan Tabel history dalam 1 halaman)
    @GetMapping("/crud-aritmatika")
    public String tampilkanHalamanCrud(Model model) {
        // Kalau barusan submit, input sebelumnya sudah ada di model (flash attribute) -> render ulang nilai itu
        if (model.containsAttribute("aritmatika")) {
            return tampilkan(model, (Aritmatika) model.getAttribute("aritmatika"));
        }
        Aritmatika baru = new Aritmatika();
        baru.setOperator("+");
        return tampilkan(model, baru);
    }

    // Mengisi form dengan data yang mau diedit
    @GetMapping("/crud-aritmatika/edit/{id}")
    public String editData(@PathVariable Long id, Model model, RedirectAttributes redirect) {
        Optional<Aritmatika> data = aritmatikaRepository.findById(id);
        if (data.isEmpty()) {
            return toast(redirect, "error", "Data #" + id + " tidak ditemukan");
        }
        return tampilkan(model, data.get());
    }

    // Menyimpan input (data baru jika id kosong, update jika id terisi). Result tidak disimpan, cuma dicek bisa dihitung.
    @PostMapping("/crud-aritmatika/simpan")
    public String simpanData(@ModelAttribute Aritmatika aritmatika, BindingResult binding,
                             RedirectAttributes redirect) {
        // Input selalu dikirim balik, jadi form tetap terisi setelah redirect (sukses maupun gagal)
        kembalikanInput(redirect, aritmatika);

        if (binding.hasErrors()) {
            return toast(redirect, "error", "Bilangan 1 dan Bilangan 2 harus berupa angka");
        }
        if (aritmatika.getBilangan1() == null || aritmatika.getBilangan2() == null) {
            return toast(redirect, "error", "Bilangan 1 dan Bilangan 2 wajib diisi");
        }
        boolean isUpdate = aritmatika.getId() != null;
        if (isUpdate && !aritmatikaRepository.existsById(aritmatika.getId())) {
            // Data sudah tidak ada: tetap kembalikan nilainya, tapi sebagai form data baru
            kembalikanInput(redirect, salinTanpaId(aritmatika));
            return toast(redirect, "error", "Data #" + aritmatika.getId() + " tidak ditemukan");
        }

        BigDecimal result;
        try {
            result = Aritmatika.hitung(aritmatika.getBilangan1(), aritmatika.getOperator(), aritmatika.getBilangan2());
        } catch (ArithmeticException | IllegalArgumentException e) {
            return toast(redirect, "error", e.getMessage());
        }
        if (!muatDiKolom(aritmatika.getBilangan1()) || !muatDiKolom(aritmatika.getBilangan2())) {
            return toast(redirect, "error", "Angka terlalu besar (maksimal 28 digit sebelum koma)");
        }
        aritmatikaRepository.save(aritmatika);
        if (!isUpdate) {
            // save() mengisi id; salin tanpa id supaya form tetap mode "data baru", bukan mode edit
            kembalikanInput(redirect, salinTanpaId(aritmatika));
        }

        String rumus = teks(aritmatika.getBilangan1()) + " " + aritmatika.getOperator() + " "
                + teks(aritmatika.getBilangan2()) + " = " + teks(result);
        return toast(redirect, "success", (isUpdate ? "Data #" + aritmatika.getId() + " diperbarui: " : "Tersimpan: ") + rumus);
    }

    // Menghapus data history
    @PostMapping("/crud-aritmatika/hapus/{id}")
    public String hapusData(@PathVariable Long id, RedirectAttributes redirect) {
        if (!aritmatikaRepository.existsById(id)) {
            return toast(redirect, "error", "Data #" + id + " tidak ditemukan");
        }
        aritmatikaRepository.deleteById(id);
        return toast(redirect, "success", "Data #" + id + " dihapus");
    }

    // Nilai yang gagal di-parse (bukan angka) jadi null; input type="number" memang tidak bisa menampilkan teks non-angka
    private void kembalikanInput(RedirectAttributes redirect, Aritmatika input) {
        redirect.addFlashAttribute("aritmatika", input);
    }

    // Result yang ditampilkan di sebelah Bilangan 2; kosong kalau input belum lengkap / tidak bisa dihitung (mis. bagi nol)
    private static String hasilUntukForm(Aritmatika form) {
        if (form.getBilangan1() == null || form.getBilangan2() == null) {
            return null;
        }
        try {
            return teks(Aritmatika.hitung(form.getBilangan1(), form.getOperator(), form.getBilangan2()));
        } catch (ArithmeticException | IllegalArgumentException e) {
            return null;
        }
    }

    private static Aritmatika salinTanpaId(Aritmatika sumber) {
        Aritmatika salinan = new Aritmatika();
        salinan.setBilangan1(sumber.getBilangan1());
        salinan.setOperator(sumber.getOperator());
        salinan.setBilangan2(sumber.getBilangan2());
        return salinan;
    }

    private String tampilkan(Model model, Aritmatika form) {
        model.addAttribute("listAritmatika", aritmatikaRepository.findAllByOrderByIdDesc());
        model.addAttribute("aritmatika", form);
        model.addAttribute("hasil", hasilUntukForm(form));
        return "CrudAritmatika";
    }

    // Pesan toast dikirim lewat flash attribute, lalu ditampilkan Sonner setelah redirect
    private String toast(RedirectAttributes redirect, String tipe, String pesan) {
        redirect.addFlashAttribute("toastType", tipe);
        redirect.addFlashAttribute("toastMessage", pesan);
        return "redirect:/crud-aritmatika";
    }

    private static boolean muatDiKolom(BigDecimal angka) {
        return angka.setScale(Aritmatika.SCALE, RoundingMode.HALF_UP).precision() <= Aritmatika.PRECISION;
    }

    private static String teks(BigDecimal angka) {
        return angka.stripTrailingZeros().toPlainString();
    }
}
