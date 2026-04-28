/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import koneksi.koneksi;
import net.sf.jasperreports.engine.JRResultSetDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.view.JasperViewer;

/**
 *
 * @author Dell
 */
public class k_kelolaPeminjamanSiswa extends javax.swing.JFrame {

    /**
     * Creates new form peminjamans_page
     */
    public k_kelolaPeminjamanSiswa() {
        this.setUndecorated(true);
        initComponents();
        setTableStyle();
        model = new DefaultTableModel();
        tblBuku.setModel(model);
        model.addColumn("Kode Buku");
        model.addColumn("Judul Buku");
        model.addColumn("Kategori");
        model.addColumn("Penulis");
        model.addColumn("Penerbit");
        model.addColumn("Asal");
        model.addColumn("Jumlah Halaman");

        model = new DefaultTableModel();
        tblPeminjaman.setModel(model);
        model.addColumn("ID Peminjaman");
        model.addColumn("NISN Siswa");
        model.addColumn("Nama SIswa");
        model.addColumn("Kode Buku");
        model.addColumn("Judul Buku");
        model.addColumn("Dipinjam Pada");
        model.addColumn("Dikembalikan Pada");
        model.addColumn("Status");
        setTanggalHariIni();
        fillSessionData();
        loadBorrowingData();

        if (cmbTahun != null) {
            cmbTahun.removeAllItems();
            cmbTahun.addItem("-- Pilih Tahun --");

            // Tambahkan 5 tahun terakhir
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            for (int i = currentYear; i >= currentYear - 5; i--) {
                cmbTahun.addItem(String.valueOf(i));
            }
        }
    }
    
    private void fillSessionData() {
        if (session.session_siswa.nisn != null) {
            txtNisn.setText(session.session_siswa.nisn);
            txtNama.setText(session.session_siswa.nameSiswa);
            
            // Set agar tidak bisa diubah manual untuk menjaga integritas data session
            txtNisn.setEditable(false); 
            txtNama.setEditable(false);
        } else {
            JOptionPane.showMessageDialog(this, "Sesi berakhir, silakan login kembali.");
            this.dispose();
        }
    }

    private DefaultTableModel model;
    ResultSet rs = null;
    PreparedStatement pst = null;
    String lblSummary;
    
    private void setTableStyle() {

   
    // STYLE tblBuku
    tblBuku.setOpaque(true);
    tblBuku.setBackground(java.awt.Color.WHITE);
    tblBuku.setForeground(java.awt.Color.BLACK);
    tblBuku.setRowHeight(25);

    tblBuku.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));

    // warna saat diklik
    tblBuku.setSelectionBackground(new java.awt.Color(0,120,215));
    tblBuku.setSelectionForeground(java.awt.Color.WHITE);

    // garis tabel
    tblBuku.setShowGrid(true);
    tblBuku.setGridColor(new java.awt.Color(200,200,200));

    // header putih
    tblBuku.getTableHeader().setBackground(java.awt.Color.WHITE);
    tblBuku.getTableHeader().setForeground(java.awt.Color.BLACK);
    tblBuku.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));


    // STYLE tblPeminjaman
    tblPeminjaman.setOpaque(true);
    tblPeminjaman.setBackground(java.awt.Color.WHITE);
    tblPeminjaman.setForeground(java.awt.Color.BLACK);
    tblPeminjaman.setRowHeight(25);

    tblPeminjaman.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));

    // warna saat diklik
    tblPeminjaman.setSelectionBackground(new java.awt.Color(0,120,215));
    tblPeminjaman.setSelectionForeground(java.awt.Color.WHITE);

    // garis tabel
    tblPeminjaman.setShowGrid(true);
    tblPeminjaman.setGridColor(new java.awt.Color(200,200,200));

    // header putih
    tblPeminjaman.getTableHeader().setBackground(java.awt.Color.WHITE);
    tblPeminjaman.getTableHeader().setForeground(java.awt.Color.BLACK);
    tblPeminjaman.getTableHeader().setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
}

    private void setTanggalHariIni() {
        java.util.Date tanggalSekarang = new java.util.Date();

        dateTanggalPinjam.setDate(tanggalSekarang);
    }
    
   
    
    private boolean validasiTanggalKembali(Date tanggalPinjam, Date tanggalKembali) {

    if (tanggalPinjam == null || tanggalKembali == null) {
        JOptionPane.showMessageDialog(null, "Tanggal tidak boleh kosong!");
        return false;
    }

    // ❌ Jika tanggal kembali sebelum tanggal pinjam
    if (tanggalKembali.before(tanggalPinjam)) {
        JOptionPane.showMessageDialog(null, 
            "Tanggal kembali tidak boleh sebelum tanggal pinjam!");
        return false;
    }

    return true;
}

    private void loadBorrowingData() {
        DefaultTableModel model = (DefaultTableModel) tblPeminjaman.getModel();
        model.setRowCount(0);

        // Pastikan ada session siswa yang aktif
        if (session.session_siswa.nisn == null) {
            JOptionPane.showMessageDialog(this, "Session tidak ditemukan. Silakan login kembali.");
            return;
        }

        try (Connection conn = koneksi.KoneksiDB()) {
            // Query dengan filter NISN siswa yang login
            String sql = "SELECT b.id, b.student_nisn, b.status, s.name AS student_name, "
                    + "GROUP_CONCAT(bd.book_code SEPARATOR ', ') AS book_codes, "
                    + "GROUP_CONCAT(bo.title SEPARATOR ', ') AS book_titles, "
                    + "b.borrowed_at, b.returned_at "
                    + "FROM borrowings b "
                    + "JOIN students s ON b.student_nisn = s.nisn "
                    + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
                    + "JOIN book_items bi ON bd.book_code = bi.code "
                    + "JOIN books bo ON bi.book_id = bo.id "
                    + "WHERE b.student_nisn = ? " // Filter berdasarkan NISN session
                    + "GROUP BY b.id, b.student_nisn, s.name, b.borrowed_at, b.returned_at, b.status "
                    + "ORDER BY b.borrowed_at DESC";

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, session.session_siswa.nisn); // Set parameter NISN dari session

            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("student_nisn"),
                    rs.getString("student_name"),
                    rs.getString("book_codes"),
                    rs.getString("book_titles"),
                    rs.getTimestamp("borrowed_at"),
                    rs.getDate("returned_at"),
                    rs.getString("status")
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal Load Data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadDetailBukuKeTable(String borrowId) {
        DefaultTableModel modelBuku = (DefaultTableModel) tblBuku.getModel();
        modelBuku.setRowCount(0); // Kosongkan tblBuku terlebih dahulu

        // Query JOIN untuk mendapatkan data buku lengkap berdasarkan ID Transaksi
        String sql = "SELECT bi.code, bo.title, c.name as category_name, "
                + "bo.author, bo.publisher, bo.origin, bo.total_pages "
                + "FROM borrowing_details bd "
                + "JOIN book_items bi ON bd.book_code = bi.code "
                + "JOIN books bo ON bi.book_id = bo.id "
                + "LEFT JOIN categories c ON bo.category_id = c.id "
                + "WHERE bd.borrowing_id = ?";

        try (Connection conn = koneksi.KoneksiDB();
                PreparedStatement pst = conn.prepareStatement(sql)) {

            pst.setString(1, borrowId);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                // Masukkan kembali ke tblBuku (form input)
                modelBuku.addRow(new Object[]{
                    rs.getString("code"), // Code Item
                    rs.getString("title"), // Judul
                    rs.getString("category_name"), // Kategori
                    rs.getString("author"), // Penulis
                    rs.getString("publisher"), // Penerbit
                    rs.getString("origin"), //  Asal
                    rs.getInt("total_pages") // Halaman
                });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat detail buku: " + e.getMessage());
        }
    }

    private void clearForm() {
        txtIdPeminjaman.setText("");
        fillSessionData();
        txtNisn.setText("");
        txtNama.setText("");
        txtKodeBuku.setText("");
        txtJudul.setText("");
        setTanggalHariIni();
        dateTanggalKembali.setDate(null);
        cmbBulan.setSelectedIndex(0);
        cmbTahun.setSelectedIndex(0);
        loadBorrowingData();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        txtNama = new javax.swing.JTextField();
        txtNisn = new javax.swing.JTextField();
        txtKodeBuku = new javax.swing.JTextField();
        txtJudul = new javax.swing.JTextField();
        dateTanggalKembali = new com.toedter.calendar.JDateChooser();
        btnUpdate = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        dateTanggalPinjam = new com.toedter.calendar.JDateChooser();
        btnCheckBuku = new javax.swing.JButton();
        btnDaftarBuku = new javax.swing.JButton();
        btnBack3 = new javax.swing.JButton();
        btnTambahBuku = new javax.swing.JButton();
        btnHapusBuku = new javax.swing.JButton();
        txtIdPeminjaman = new javax.swing.JTextField();
        cmbTahun = new javax.swing.JComboBox<>();
        btnFilterTahun = new javax.swing.JButton();
        cmbBulan = new javax.swing.JComboBox<>();
        btnFilterBulan = new javax.swing.JButton();
        btnFilterBulanDanTahun = new javax.swing.JButton();
        btnCetakStruk = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblBuku = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblPeminjaman = new javax.swing.JTable();
        btnRiwayatSiswa = new javax.swing.JButton();
        btnKelolaPengembalian = new javax.swing.JButton();
        btnKelolaPeminjaman = new javax.swing.JButton();
        btnDashboard = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        lblNotif = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txtNama.setBackground(new java.awt.Color(0,0,0,0));
        txtNama.setBorder(null);
        txtNama.setEnabled(false);
        jPanel1.add(txtNama, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 430, 380, 20));

        txtNisn.setBackground(new java.awt.Color(0,0,0,0));
        txtNisn.setBorder(null);
        jPanel1.add(txtNisn, new org.netbeans.lib.awtextra.AbsoluteConstraints(1100, 330, 150, 20));

        txtKodeBuku.setBackground(new java.awt.Color(0,0,0,0));
        txtKodeBuku.setBorder(null);
        jPanel1.add(txtKodeBuku, new org.netbeans.lib.awtextra.AbsoluteConstraints(1110, 190, 140, 20));

        txtJudul.setBackground(new java.awt.Color(0,0,0,0));
        txtJudul.setBorder(null);
        txtJudul.setEnabled(false);
        jPanel1.add(txtJudul, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 190, 210, 20));
        jPanel1.add(dateTanggalKembali, new org.netbeans.lib.awtextra.AbsoluteConstraints(1050, 510, 220, 50));

        btnUpdate.setBackground(new java.awt.Color(0,0,0,0));
        btnUpdate.setBorder(null);
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });
        jPanel1.add(btnUpdate, new org.netbeans.lib.awtextra.AbsoluteConstraints(960, 580, 100, 40));

        btnRefresh.setBackground(new java.awt.Color(0,0,0,0));
        btnRefresh.setBorder(null);
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });
        jPanel1.add(btnRefresh, new org.netbeans.lib.awtextra.AbsoluteConstraints(1070, 580, 100, 40));

        btnDelete.setBackground(new java.awt.Color(0,0,0,0));
        btnDelete.setBorder(null);
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        jPanel1.add(btnDelete, new org.netbeans.lib.awtextra.AbsoluteConstraints(1180, 580, 100, 50));

        btnSave.setBackground(new java.awt.Color(0,0,0,0));
        btnSave.setBorder(null);
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        jPanel1.add(btnSave, new org.netbeans.lib.awtextra.AbsoluteConstraints(840, 580, 110, 40));

        dateTanggalPinjam.setDateFormatString("d MMM, yyyy");
        dateTanggalPinjam.setEnabled(false);
        jPanel1.add(dateTanggalPinjam, new org.netbeans.lib.awtextra.AbsoluteConstraints(850, 510, 190, 50));

        btnCheckBuku.setBackground(new java.awt.Color(0,0,0,0));
        btnCheckBuku.setBorder(null);
        btnCheckBuku.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCheckBukuActionPerformed(evt);
            }
        });
        jPanel1.add(btnCheckBuku, new org.netbeans.lib.awtextra.AbsoluteConstraints(1260, 180, 70, 40));

        btnDaftarBuku.setBackground(new java.awt.Color(0,0,0,0));
        btnDaftarBuku.setBorder(null);
        btnDaftarBuku.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDaftarBukuActionPerformed(evt);
            }
        });
        jPanel1.add(btnDaftarBuku, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 130, 520, 40));

        btnBack3.setBackground(new java.awt.Color(0,0,0,0));
        btnBack3.setBorder(null);
        btnBack3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBack3ActionPerformed(evt);
            }
        });
        jPanel1.add(btnBack3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 690, 200, 50));

        btnTambahBuku.setBackground(new java.awt.Color(0,0,0,0));
        btnTambahBuku.setBorder(null);
        btnTambahBuku.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnTambahBukuActionPerformed(evt);
            }
        });
        jPanel1.add(btnTambahBuku, new org.netbeans.lib.awtextra.AbsoluteConstraints(850, 240, 410, 30));

        btnHapusBuku.setBackground(new java.awt.Color(0,0,0,0));
        btnHapusBuku.setBorder(null);
        btnHapusBuku.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHapusBukuActionPerformed(evt);
            }
        });
        jPanel1.add(btnHapusBuku, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 390, 520, 40));

        txtIdPeminjaman.setBackground(new java.awt.Color(0,0,0,0));
        txtIdPeminjaman.setBorder(null);
        jPanel1.add(txtIdPeminjaman, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 330, 200, 20));

        cmbTahun.setBackground(new java.awt.Color(0, 0, 0, 0));
        cmbTahun.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        cmbTahun.setBorder(null);
        jPanel1.add(cmbTahun, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 470, 140, -1));

        btnFilterTahun.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnFilterTahun.setBorder(null);
        btnFilterTahun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterTahunActionPerformed(evt);
            }
        });
        jPanel1.add(btnFilterTahun, new org.netbeans.lib.awtextra.AbsoluteConstraints(630, 470, 30, 30));

        cmbBulan.setBackground(new java.awt.Color(0, 0, 0, 0));
        cmbBulan.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        cmbBulan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-- Pilih Bulan --", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember", " " }));
        cmbBulan.setBorder(null);
        jPanel1.add(cmbBulan, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 470, 140, 20));

        btnFilterBulan.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnFilterBulan.setBorder(null);
        btnFilterBulan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterBulanActionPerformed(evt);
            }
        });
        jPanel1.add(btnFilterBulan, new org.netbeans.lib.awtextra.AbsoluteConstraints(450, 470, 30, 30));

        btnFilterBulanDanTahun.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnFilterBulanDanTahun.setBorder(null);
        btnFilterBulanDanTahun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterBulanDanTahunActionPerformed(evt);
            }
        });
        jPanel1.add(btnFilterBulanDanTahun, new org.netbeans.lib.awtextra.AbsoluteConstraints(670, 470, 140, 30));

        btnCetakStruk.setBackground(new java.awt.Color(0,0,0,0));
        btnCetakStruk.setBorder(null);
        btnCetakStruk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetakStrukActionPerformed(evt);
            }
        });
        jPanel1.add(btnCetakStruk, new org.netbeans.lib.awtextra.AbsoluteConstraints(840, 650, 440, 40));

        tblBuku.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        tblBuku.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblBukuMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                tblBukuMouseEntered(evt);
            }
        });
        jScrollPane1.setViewportView(tblBuku);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 210, 500, 160));

        tblPeminjaman.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "ID Peminjaman", "NISN Siswa", "Nama Siswa", "Kode Buku", "Judul Buku", "Dipinjam pada", "Dikembalikan pada", "Status"
            }
        ));
        tblPeminjaman.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblPeminjamanMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                tblPeminjamanMouseEntered(evt);
            }
        });
        jScrollPane2.setViewportView(tblPeminjaman);

        jPanel1.add(jScrollPane2, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 510, 490, 170));

        btnRiwayatSiswa.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnRiwayatSiswa.setBorder(null);
        btnRiwayatSiswa.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRiwayatSiswaActionPerformed(evt);
            }
        });
        jPanel1.add(btnRiwayatSiswa, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 280, 240, 30));

        btnKelolaPengembalian.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnKelolaPengembalian.setBorder(null);
        btnKelolaPengembalian.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKelolaPengembalianActionPerformed(evt);
            }
        });
        jPanel1.add(btnKelolaPengembalian, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 230, 240, 30));

        btnKelolaPeminjaman.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnKelolaPeminjaman.setBorder(null);
        btnKelolaPeminjaman.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKelolaPeminjamanActionPerformed(evt);
            }
        });
        jPanel1.add(btnKelolaPeminjaman, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 190, 240, 30));

        btnDashboard.setBackground(new java.awt.Color(0, 0, 0, 0));
        btnDashboard.setBorder(null);
        btnDashboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDashboardActionPerformed(evt);
            }
        });
        jPanel1.add(btnDashboard, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 140, 240, 40));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image/PINJAM BUKU - SISWA (3).png"))); // NOI18N
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        lblNotif.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image/PINJAM BUKU - SISWA (1).png"))); // NOI18N
        lblNotif.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblNotifMouseClicked(evt);
            }
        });
        jPanel1.add(lblNotif, new org.netbeans.lib.awtextra.AbsoluteConstraints(1260, 10, 90, 70));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnCheckBukuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCheckBukuActionPerformed
        // TODO add your handling code here:
        // Ambil input dari TextField (kode dari book_items)
        String inputKode = txtKodeBuku.getText().trim();

        // Validasi jika input kosong
        if (inputKode.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan Kode Buku!");
            txtKodeBuku.requestFocus();
            return;
        }

        try {
            Connection conn = koneksi.KoneksiDB();

            // Query untuk mengambil judul buku berdasarkan kode item
            // JOIN antara book_items dan books
            String sql = "SELECT b.title FROM book_items bi "
                    + "INNER JOIN books b ON bi.book_id = b.id "
                    + "WHERE bi.code = ?";
            PreparedStatement pst = conn.prepareStatement(sql);

            // Menggunakan setString karena tipe data code di database adalah VARCHAR
            pst.setString(1, inputKode);

            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                // Jika ditemukan, set teks ke txtJudul
                String judulBuku = rs.getString("title");
                txtJudul.setText(judulBuku);
            } else {
                // Jika tidak ditemukan
                JOptionPane.showMessageDialog(this, "Item dengan kode '" + inputKode + "' tidak ditemukan!");
                txtJudul.setText(""); // Kosongkan jika sebelumnya ada isi
                txtKodeBuku.requestFocus();
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error Database: " + e.getMessage());
        }
    }//GEN-LAST:event_btnCheckBukuActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        // TODO add your handling code here:
        String nisn = txtNisn.getText().trim();
        java.util.Date tglHarusKembali = dateTanggalKembali.getDate();
        DefaultTableModel model = (DefaultTableModel) tblBuku.getModel();

        if (nisn.isEmpty() || tglHarusKembali == null || model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Data belum lengkap!");
            return;
        }
         Date tanggalPinjam = dateTanggalPinjam.getDate();
        Date tanggalKembali = dateTanggalKembali.getDate();

      if (!validasiTanggalKembali(tanggalPinjam, tanggalKembali)) {
          return; // STOP proses save
       }

        Connection conn = null;
        try {
            conn = koneksi.KoneksiDB();
            conn.setAutoCommit(false); // Memulai Transaksi

            // Simpan ke tabel 'borrowings'
            // Kolom: student_nisn, borrowed_at, returned_at (ini tgl deadline)
            String sqlMaster = "INSERT INTO borrowings (student_nisn, borrowed_at, returned_at) VALUES (?, NOW(), ?)";
            PreparedStatement pstMaster = conn.prepareStatement(sqlMaster, Statement.RETURN_GENERATED_KEYS);
            pstMaster.setString(1, nisn);
            pstMaster.setDate(2, new java.sql.Date(tglHarusKembali.getTime()));
            pstMaster.executeUpdate();

            ResultSet rsKey = pstMaster.getGeneratedKeys();
            if (!rsKey.next()) {
                throw new Exception("Gagal mendapatkan ID Peminjaman.");
            }
            int borrowId = rsKey.getInt(1);

            // Persiapkan Query Detail dan Update Status
            // Sesuai tabel kamu: borrowing_details (borrowing_id, book_code)
            String sqlDetail = "INSERT INTO borrowing_details (borrowing_id, book_code) VALUES (?, ?)";
            String sqlUpdateStatus = "UPDATE book_items SET status = 'process' WHERE code = ?";

            PreparedStatement pstDetail = conn.prepareStatement(sqlDetail);
            PreparedStatement pstUpdate = conn.prepareStatement(sqlUpdateStatus);

            // Loop isi tabel untuk insert detail
            for (int i = 0; i < model.getRowCount(); i++) {
                String itemCode = model.getValueAt(i, 0).toString(); // Ambil Code dari kolom 0

                // Simpan Detail
                pstDetail.setInt(1, borrowId);
                pstDetail.setString(2, itemCode);
                pstDetail.executeUpdate();

                // Update status buku fisik tersebut
                pstUpdate.setString(1, itemCode);
                pstUpdate.executeUpdate();
            }

            conn.commit(); // Eksekusi sukses semua
            JOptionPane.showMessageDialog(this, "Peminjaman Berhasil Disimpan!");

            model.setRowCount(0); // Kosongkan daftar buku
            clearForm(); // Reset field NISN dan Tanggal
            loadBorrowingData();

        } catch (Exception e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                }
            }
            JOptionPane.showMessageDialog(this, "Gagal Simpan: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        // TODO add your handling code here:
        String borrowId = txtIdPeminjaman.getText(); // ID transaksi peminjaman
        String nisn = txtNisn.getText();
        java.util.Date tglKembali = dateTanggalKembali.getDate();
        DefaultTableModel model = (DefaultTableModel) tblBuku.getModel();

        if (borrowId.isEmpty() || model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Pilih data dan pastikan daftar buku tidak kosong!");
            return;
        }
        
        Date tanggalPinjam = dateTanggalPinjam.getDate();
        Date tanggalKembali = dateTanggalKembali.getDate();

        if (!validasiTanggalKembali(tanggalPinjam, tanggalKembali)) {
        return; // STOP update
           }

        Connection conn = null;
        try {
            conn = koneksi.KoneksiDB();
            conn.setAutoCommit(false);

            // VALIDASI: CEK STATUS PEMINJAMAN SEBELUM UPDATE
            String sqlCheckStatus = "SELECT status FROM borrowings WHERE id = ?";
            PreparedStatement pstCheckStatus = conn.prepareStatement(sqlCheckStatus);
            pstCheckStatus.setString(1, borrowId);
            ResultSet rsStatus = pstCheckStatus.executeQuery();

            if (rsStatus.next()) {
                String currentStatus = rsStatus.getString("status");
                if ("approved".equalsIgnoreCase(currentStatus) || "rejected".equalsIgnoreCase(currentStatus)) {
                    JOptionPane.showMessageDialog(this,
                            "Tidak dapat menghapus peminjaman yang telah ditolak atau disetujui!\n"
                            + "Status saat ini: " + currentStatus.toUpperCase());
                    conn.rollback();
                    return;
                }
            } else {
                JOptionPane.showMessageDialog(this, "Data peminjaman tidak ditemukan!");
                conn.rollback();
                return;
            }

            // KEMBALIKAN STATUS BUKU LAMA
            // Ambil dulu semua buku yang terdaftar di database sebelum diupdate
            String sqlGetOld = "SELECT book_code FROM borrowing_details WHERE borrowing_id = ?";
            PreparedStatement pstGetOld = conn.prepareStatement(sqlGetOld);
            pstGetOld.setString(1, borrowId);
            ResultSet rsOld = pstGetOld.executeQuery();

            String sqlRestore = "UPDATE book_items SET status = 'available' WHERE code = ?";
            PreparedStatement pstRestore = conn.prepareStatement(sqlRestore);
            while (rsOld.next()) {
                pstRestore.setString(1, rsOld.getString("book_code"));
                pstRestore.executeUpdate();
            }

            // HAPUS SEMUA DETAIL LAMA
            String sqlDeleteDetail = "DELETE FROM borrowing_details WHERE borrowing_id = ?";
            PreparedStatement pstDel = conn.prepareStatement(sqlDeleteDetail);
            pstDel.setString(1, borrowId);
            pstDel.executeUpdate();

            // UPDATE DATA MASTER (NISN & Tanggal Deadline)
            String sqlUpdateMaster = "UPDATE borrowings SET student_nisn = ?, returned_at = ? WHERE id = ?";
            PreparedStatement pstMaster = conn.prepareStatement(sqlUpdateMaster);
            pstMaster.setString(1, nisn);
            pstMaster.setDate(2, new java.sql.Date(tglKembali.getTime()));
            pstMaster.setString(3, borrowId);
            pstMaster.executeUpdate();

            // INSERT DAFTAR BUKU BARU & UPDATE STATUSNYA
            String sqlInsertNew = "INSERT INTO borrowing_details (borrowing_id, book_code) VALUES (?, ?)";
            String sqlSetBorrowed = "UPDATE book_items SET status = 'process' WHERE code = ?";

            PreparedStatement pstInsert = conn.prepareStatement(sqlInsertNew);
            PreparedStatement pstUpdateStatus = conn.prepareStatement(sqlSetBorrowed);

            for (int i = 0; i < model.getRowCount(); i++) {
                String itemCode = model.getValueAt(i, 0).toString();

                // Insert detail baru
                pstInsert.setString(1, borrowId);
                pstInsert.setString(2, itemCode);
                pstInsert.executeUpdate();

                // Set status buku tersebut jadi borrowed
                pstUpdateStatus.setString(1, itemCode);
                pstUpdateStatus.executeUpdate();
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, "Transaksi Berhasil Diperbarui!");
            loadBorrowingData();

        } catch (Exception e) {
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException ex) {
            }
            JOptionPane.showMessageDialog(this, "Gagal Update: " + e.getMessage());
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException ex) {
            }
        }
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        // TODO add your handling code here:
        clearForm();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        // TODO add your handling code here:
        String borrowId = txtIdPeminjaman.getText();

        if (borrowId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data yang ingin dihapus!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Hapus transaksi ini? Status buku akan dikembalikan ke 'available'.",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = null;
            try {
                conn = koneksi.KoneksiDB();
                conn.setAutoCommit(false);

                // VALIDASI: CEK STATUS PEMINJAMAN SEBELUM HAPUS
                String sqlCheckStatus = "SELECT status FROM borrowings WHERE id = ?";
                PreparedStatement pstCheckStatus = conn.prepareStatement(sqlCheckStatus);
                pstCheckStatus.setString(1, borrowId);
                ResultSet rsStatus = pstCheckStatus.executeQuery();

                if (rsStatus.next()) {
                    String currentStatus = rsStatus.getString("status");
                    if ("approved".equalsIgnoreCase(currentStatus) || "rejected".equalsIgnoreCase(currentStatus)) {
                        JOptionPane.showMessageDialog(this,
                                "Tidak dapat menghapus peminjaman yang telah ditolak atau disetujui!\n"
                                + "Status saat ini: " + currentStatus.toUpperCase());
                        conn.rollback();
                        return;
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "Data peminjaman tidak ditemukan!");
                    conn.rollback();
                    return;
                }

                // Ambil semua kode buku yang ada di detail peminjaman ini
                String sqlGetItems = "SELECT book_code FROM borrowing_details WHERE borrowing_id = ?";
                PreparedStatement pstGet = conn.prepareStatement(sqlGetItems);
                pstGet.setString(1, borrowId);
                ResultSet rs = pstGet.executeQuery();

                // Siapkan statement untuk update status buku kembali ke 'available'
                String sqlRestore = "UPDATE book_items SET status = 'available' WHERE code = ?";
                PreparedStatement pstRestore = conn.prepareStatement(sqlRestore);

                boolean hasBooks = false;
                while (rs.next()) {
                    pstRestore.setString(1, rs.getString("book_code"));
                    pstRestore.executeUpdate();
                    hasBooks = true;
                }

                // Hapus detail peminjaman (karena FK, harus detail dulu baru master)
                String sqlDelDetail = "DELETE FROM borrowing_details WHERE borrowing_id = ?";
                PreparedStatement pstDelDetail = conn.prepareStatement(sqlDelDetail);
                pstDelDetail.setString(1, borrowId);
                pstDelDetail.executeUpdate();

                // Hapus master peminjaman
                String sqlDelMaster = "DELETE FROM borrowings WHERE id = ?";
                PreparedStatement pstDelMaster = conn.prepareStatement(sqlDelMaster);
                pstDelMaster.setString(1, borrowId);
                int affectedRows = pstDelMaster.executeUpdate();

                conn.commit();

                String message = "Transaksi berhasil dibatalkan";
                if (hasBooks) {
                    message += " dan stok buku dikembalikan";
                }
                message += "!";

                JOptionPane.showMessageDialog(this, message);
                loadBorrowingData();
                clearForm();

            } catch (Exception e) {
                try {
                    if (conn != null) {
                        conn.rollback();
                    }
                } catch (SQLException ex) {
                }
                JOptionPane.showMessageDialog(this, "Gagal Hapus: " + e.getMessage());
            } finally {
                try {
                    if (conn != null) {
                        conn.setAutoCommit(true);
                        conn.close();
                    }
                } catch (SQLException ex) {
                }
            }
        }
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnDaftarBukuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDaftarBukuActionPerformed
        // TODO add your handling code here:
        g_daftarStockBuku obj = new g_daftarStockBuku();
        obj.setVisible(true);
    }//GEN-LAST:event_btnDaftarBukuActionPerformed

    private void btnBack3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBack3ActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_btnBack3ActionPerformed

    private void tblBukuMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblBukuMouseClicked
        // TODO add your handling code here:
    }//GEN-LAST:event_tblBukuMouseClicked

    private void tblBukuMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblBukuMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_tblBukuMouseEntered

    private void btnTambahBukuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnTambahBukuActionPerformed
        // TODO add your handling code here:
        String itemCode = txtKodeBuku.getText().trim();
        String judul = txtJudul.getText().trim();

        if (itemCode.isEmpty() || judul.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Masukkan Kode Item Buku!");
            return;
        }

        try {
            Connection conn = koneksi.KoneksiDB();

            // Query Lengkap: Mengambil detail item fisik dan metadata bukunya
            String sql = "SELECT bi.code, bi.status, b.title, c.name as category_name, "
                    + "b.author, b.publisher, b.origin, b.total_pages "
                    + "FROM book_items bi "
                    + "JOIN books b ON bi.book_id = b.id "
                    + "LEFT JOIN categories c ON b.category_id = c.id "
                    + "WHERE bi.code = ?";

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, itemCode);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String status = rs.getString("status");

                // Cegah Duplikat (Satu fisik buku tidak bisa dimasukkan dua kali ke daftar)
                for (int i = 0; i < model.getRowCount(); i++) {
                    if (model.getValueAt(i, 0).toString().equals(itemCode)) {
                        JOptionPane.showMessageDialog(this, "Item ini sudah ada di daftar pinjam!");
                        return;
                    }
                }

                // Validasi Status (Hanya 'available' yang bisa dipinjam)
                if (status.equalsIgnoreCase("borrowed")) {
                    JOptionPane.showMessageDialog(this,
                            "Buku dengan kode " + itemCode + " sedang dipinjam (Status: BORROWED)");
                    return;
                }

                if (status.equalsIgnoreCase("process")) {
                    JOptionPane.showMessageDialog(this,
                            "Buku dengan kode " + itemCode + " sedang dalam proses (Status: PROCESS)");
                    return;
                }

                DefaultTableModel model = (DefaultTableModel) tblBuku.getModel();

                // Validasi maksimal jumlah buku dalam satu peminjaman (opsional)
                int maxBooks = 3; // Atur batas maksimal
                if (model.getRowCount() >= maxBooks) {
                    JOptionPane.showMessageDialog(this,
                            "Tidak dapat menambahkan lebih dari " + maxBooks + " buku dalam satu peminjaman!");
                    return;
                }

                // Masukkan data lengkap ke tabel (Sesuaikan urutan kolom model Anda)
                model.addRow(new Object[]{
                    rs.getString("code"), // Code Item
                    rs.getString("title"), // Judul
                    rs.getString("category_name"), // Kategori
                    rs.getString("author"), // Penulis
                    rs.getString("publisher"), // Penerbit
                    rs.getString("origin"), // Asal
                    rs.getInt("total_pages") // Halaman
                });

                // Bersihkan input
                txtKodeBuku.setText("");
                txtJudul.setText("");
                txtKodeBuku.requestFocus();

            } else {
                JOptionPane.showMessageDialog(this, "Kode Item '" + itemCode + "' tidak ditemukan di database!");
            }

            conn.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }//GEN-LAST:event_btnTambahBukuActionPerformed

    private void btnHapusBukuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusBukuActionPerformed
        // TODO add your handling code here:
        int selectedRow = tblBuku.getSelectedRow();
        if (selectedRow != -1) {
            DefaultTableModel model = (DefaultTableModel) tblBuku.getModel();
            model.removeRow(selectedRow);
        } else {
            JOptionPane.showMessageDialog(this, "Pilih baris di tabel yang ingin dihapus!");
        }
    }//GEN-LAST:event_btnHapusBukuActionPerformed

    private void tblPeminjamanMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblPeminjamanMouseClicked
        // TODO add your handling code here:
        int row = tblPeminjaman.getSelectedRow();
        if (row != -1) {
            // Mengisi Field Input Utama
            String idPinjam = tblPeminjaman.getValueAt(row, 0).toString();
            String nisn = tblPeminjaman.getValueAt(row, 1).toString();

            txtIdPeminjaman.setText(idPinjam);
            txtNisn.setText(nisn);

            // Ambil tanggal kembali (deadline) dari tabel (kolom index 6)
            try {
                Object tglObject = tblPeminjaman.getValueAt(row, 6);
                if (tglObject instanceof java.util.Date) {
                    dateTanggalKembali.setDate((java.util.Date) tglObject);
                }

                Object tglObject2 = tblPeminjaman.getValueAt(row, 5);
                if (tglObject instanceof java.util.Date) {
                    dateTanggalPinjam.setDate((java.util.Date) tglObject2);
                }
            } catch (Exception e) {
                System.err.println("Gagal set tanggal: " + e.getMessage());
            }

            //  Load detail buku ke tblBuku menggunakan query database (Bukan split string)
            loadDetailBukuKeTable(idPinjam);
        }
    }//GEN-LAST:event_tblPeminjamanMouseClicked

    private void tblPeminjamanMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblPeminjamanMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_tblPeminjamanMouseEntered

    private void btnFilterTahunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterTahunActionPerformed
        // TODO add your handling code here:
        int indexTahun = cmbTahun.getSelectedIndex();

        // Index 0 = belum pilih tahun
        if (indexTahun == 0) {
            JOptionPane.showMessageDialog(this,
                    "Harap pilih tahun terlebih dahulu!",
                    "Peringatan",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Pastikan ada session siswa yang aktif
        if (session.session_siswa.nisn == null) {
            JOptionPane.showMessageDialog(this, "Session tidak ditemukan. Silakan login kembali.");
            return;
        }

        int tahun = Integer.parseInt(cmbTahun.getSelectedItem().toString());

        DefaultTableModel model = (DefaultTableModel) tblPeminjaman.getModel();
        model.setRowCount(0);

        try (Connection conn = koneksi.KoneksiDB()) {

            String sql = "SELECT b.id, b.student_nisn, b.status, s.name AS student_name, "
                    + "GROUP_CONCAT(bd.book_code SEPARATOR ', ') AS book_codes, "
                    + "GROUP_CONCAT(bo.title SEPARATOR ', ') AS book_titles, "
                    + "b.borrowed_at, b.returned_at "
                    + "FROM borrowings b "
                    + "JOIN students s ON b.student_nisn = s.nisn "
                    + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
                    + "JOIN book_items bi ON bd.book_code = bi.code "
                    + "JOIN books bo ON bi.book_id = bo.id "
                    + "WHERE YEAR(b.borrowed_at) = ? AND b.student_nisn = ? " // Tambah filter NISN
                    + "GROUP BY b.id, b.student_nisn, s.name, b.borrowed_at, b.returned_at, b.status "
                    + "ORDER BY b.borrowed_at DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, tahun);
            ps.setString(2, session.session_siswa.nisn); // Set NISN dari session

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("student_nisn"),
                    rs.getString("student_name"),
                    rs.getString("book_codes"),
                    rs.getString("book_titles"),
                    rs.getTimestamp("borrowed_at"),
                    rs.getDate("returned_at"),
                    rs.getString("status")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal filter data: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnFilterTahunActionPerformed

    private void btnFilterBulanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterBulanActionPerformed
        // TODO add your handling code here:
        int indexBulan = cmbBulan.getSelectedIndex();

        // Index 0 = belum pilih bulan
        if (indexBulan == 0) {
            JOptionPane.showMessageDialog(this,
                    "Harap pilih bulan terlebih dahulu!",
                    "Peringatan",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Pastikan ada session siswa yang aktif
        if (session.session_siswa.nisn == null) {
            JOptionPane.showMessageDialog(this, "Session tidak ditemukan. Silakan login kembali.");
            return;
        }

        int bulan = indexBulan; // karena Januari = index 1

        DefaultTableModel model = (DefaultTableModel) tblPeminjaman.getModel();
        model.setRowCount(0);

        try (Connection conn = koneksi.KoneksiDB()) {

            String sql = "SELECT b.id, b.student_nisn, b.status, s.name AS student_name, "
                    + "GROUP_CONCAT(bd.book_code SEPARATOR ', ') AS book_codes, "
                    + "GROUP_CONCAT(bo.title SEPARATOR ', ') AS book_titles, "
                    + "b.borrowed_at, b.returned_at "
                    + "FROM borrowings b "
                    + "JOIN students s ON b.student_nisn = s.nisn "
                    + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
                    + "JOIN book_items bi ON bd.book_code = bi.code "
                    + "JOIN books bo ON bi.book_id = bo.id "
                    + "WHERE MONTH(b.borrowed_at) = ? AND b.student_nisn = ? " // Tambah filter NISN
                    + "GROUP BY b.id, b.student_nisn, s.name, b.borrowed_at, b.returned_at, b.status "
                    + "ORDER BY b.borrowed_at DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, bulan);
            ps.setString(2, session.session_siswa.nisn); // Set NISN dari session

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("student_nisn"),
                    rs.getString("student_name"),
                    rs.getString("book_codes"),
                    rs.getString("book_titles"),
                    rs.getTimestamp("borrowed_at"),
                    rs.getDate("returned_at"),
                    rs.getString("status")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal filter data: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnFilterBulanActionPerformed

    private void btnFilterBulanDanTahunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterBulanDanTahunActionPerformed
        // TODO add your handling code here:
        int indexBulan = cmbBulan.getSelectedIndex();
        int indexTahun = cmbTahun.getSelectedIndex();

        // Validasi pilihan
        if (indexBulan == 0 || indexTahun == 0) {
            JOptionPane.showMessageDialog(this,
                    "Harap pilih bulan dan tahun terlebih dahulu!",
                    "Peringatan",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Pastikan ada session siswa yang aktif
        if (session.session_siswa.nisn == null) {
            JOptionPane.showMessageDialog(this, "Session tidak ditemukan. Silakan login kembali.");
            return;
        }

        int bulan = indexBulan; // Januari = 1
        int tahun = Integer.parseInt(cmbTahun.getSelectedItem().toString());

        DefaultTableModel model = (DefaultTableModel) tblPeminjaman.getModel();
        model.setRowCount(0);

        try (Connection conn = koneksi.KoneksiDB()) {

            String sql = "SELECT b.id, b.student_nisn, b.status, s.name AS student_name, "
                    + "GROUP_CONCAT(bd.book_code SEPARATOR ', ') AS book_codes, "
                    + "GROUP_CONCAT(bo.title SEPARATOR ', ') AS book_titles, "
                    + "b.borrowed_at, b.returned_at "
                    + "FROM borrowings b "
                    + "JOIN students s ON b.student_nisn = s.nisn "
                    + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
                    + "JOIN book_items bi ON bd.book_code = bi.code "
                    + "JOIN books bo ON bi.book_id = bo.id "
                    + "WHERE MONTH(b.borrowed_at) = ? AND YEAR(b.borrowed_at) = ? AND b.student_nisn = ? " // Tambah filter NISN
                    + "GROUP BY b.id, b.student_nisn, s.name, b.borrowed_at, b.returned_at, b.status "
                    + "ORDER BY b.borrowed_at DESC";

            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, bulan);
            ps.setInt(2, tahun);
            ps.setString(3, session.session_siswa.nisn); // Set NISN dari session

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("student_nisn"),
                    rs.getString("student_name"),
                    rs.getString("book_codes"),
                    rs.getString("book_titles"),
                    rs.getTimestamp("borrowed_at"),
                    rs.getDate("returned_at"),
                    rs.getString("status")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal filter data: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnFilterBulanDanTahunActionPerformed

    private void btnCetakStrukActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakStrukActionPerformed
        // TODO add your handling code here:
        if (txtIdPeminjaman.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Masukkan ID Peminjaman terlebih dahulu!");
            return;
        }

        // Ambil ID dari TextField
        int idPeminjaman;
        try {
            idPeminjaman = Integer.parseInt(txtIdPeminjaman.getText());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "ID Peminjaman harus berupa angka!");
            return;
        }

        // Path ke file report jrxml (sesuaikan dengan lokasi Anda)
        String reportPath = "src/report/a_strukPeminjaman.jrxml";

        // Query utama untuk mendapatkan data peminjaman
        String sql = "SELECT "
        + "students.`nisn` AS students_nisn, "
        + "students.`name` AS students_name, "
        + "students.`password` AS students_password, "
        + "students.`classroom` AS students_classroom, "
        + "students.`phone_number` AS students_phone_number, "
        + "students.`address` AS students_address, "
        + "categories.`id` AS categories_id, "
        + "categories.`name` AS categories_name, "
        + "borrowings.`id` AS borrowings_id, "
        + "borrowings.`student_nisn` AS borrowings_student_nisn, "
        + "borrowings.`borrowed_at` AS borrowings_borrowed_at, "
        + "borrowings.`returned_at` AS borrowings_returned_at, "
        + "borrowings.`status` AS borrowings_status, "
        + "borrowings.`created_at` AS borrowings_created_at, "
        + "borrowing_details.`id` AS borrowing_details_id, "
        + "borrowing_details.`borrowing_id` AS borrowing_details_borrowing_id, "
        + "borrowing_details.`book_code` AS borrowing_details_book_code, "
        + "books.`id` AS books_id, "
        + "books.`title` AS books_title, "
        + "books.`author` AS books_author, "
        + "books.`publisher` AS books_publisher, "
        + "books.`origin` AS books_origin, "
        + "books.`total_pages` AS books_total_pages, "
        + "books.`cover` AS books_cover, "
        + "books.`category_id` AS books_category_id, "
        + "book_items.`code` AS book_items_code, "
        + "book_items.`book_id` AS book_items_book_id, "
        + "book_items.`status` AS book_items_status, "
        + "admins.`id` AS admins_id, "
        + "admins.`name` AS admins_name, "
        + "admins.`username` AS admins_username, "
        + "admins.`password` AS admins_password "
        + "FROM `students` students "
        + "INNER JOIN `borrowings` borrowings ON students.`nisn` = borrowings.`student_nisn` "
        + "INNER JOIN `borrowing_details` borrowing_details ON borrowings.`id` = borrowing_details.`borrowing_id` "
        + "INNER JOIN `book_items` book_items ON borrowing_details.`book_code` = book_items.`code` "
        + "INNER JOIN `books` books ON book_items.`book_id` = books.`id` "
        + "INNER JOIN `categories` categories ON books.`category_id` = categories.`id` "
        + "INNER JOIN `admins` admins "
        + "WHERE borrowings.id = ?";

        Connection conn = null;
        try {
            // Eksekusi query
            conn = koneksi.KoneksiDB();
            pst = conn.prepareStatement(sql);
            pst.setInt(1, idPeminjaman);
            ResultSet rp = pst.executeQuery();

            // Cek apakah ada data peminjaman yang ditemukan
            if (!rp.isBeforeFirst()) {
                JOptionPane.showMessageDialog(null, "ID Peminjaman tidak ditemukan!");
                return;
            }

            // Menampilkan laporan
            JRResultSetDataSource jrRS = new JRResultSetDataSource(rp);
            JasperReport jasperReport = JasperCompileManager.compileReport(reportPath);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, null, jrRS);

            // Menampilkan laporan
            JRViewer aViewer = new JRViewer(jasperPrint);
            JDialog viewer = new JDialog();
            viewer.setTitle("Struk Peminjaman Buku");
            viewer.setAlwaysOnTop(true);
            viewer.getContentPane().add(aViewer);
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            viewer.setBounds(0, 0, screenSize.width, screenSize.height);
            viewer.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal mengambil data peminjaman: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Gagal menampilkan laporan: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnCetakStrukActionPerformed

    private void btnRiwayatSiswaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRiwayatSiswaActionPerformed
        // TODO add your handling code here:
        k_riwayatSiswa obj = new   k_riwayatSiswa();
        obj.setVisible(true);
    }//GEN-LAST:event_btnRiwayatSiswaActionPerformed

    private void btnKelolaPengembalianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKelolaPengembalianActionPerformed
        // TODO add your handling code here:
        k_kelolaPengembalianSiswa obj = new k_kelolaPengembalianSiswa();
        obj.setVisible(true);
    }//GEN-LAST:event_btnKelolaPengembalianActionPerformed

    private void btnKelolaPeminjamanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKelolaPeminjamanActionPerformed
        // TODO add your handling code here:
        k_kelolaPeminjamanSiswa obj = new k_kelolaPeminjamanSiswa();
        obj.setVisible(true);
    }//GEN-LAST:event_btnKelolaPeminjamanActionPerformed

    private void btnDashboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDashboardActionPerformed
        // TODO add your handling code here:
        d_dashboardSiswa obj = new  d_dashboardSiswa();
        obj.setVisible(true);
    }//GEN-LAST:event_btnDashboardActionPerformed

    private void lblNotifMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblNotifMouseClicked

        // 1. Cek apakah ada session (keamanan tambahan)
        if (session.session_siswa.nisn == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "Silahkan login terlebih dahulu");
            return;
        }

        // 2. Panggil DialogNotifikasi
        // Gunakan 'this' agar pop-up mereferensi ke frame utama
        DialogNotifikasi nav = new DialogNotifikasi(new javax.swing.JFrame(), false);

        // 3. Atur posisi agar muncul tepat di bawah icon lonceng
        int x = lblNotif.getLocationOnScreen().x - 280; // Sesuaikan agar pas
        int y = lblNotif.getLocationOnScreen().y + lblNotif.getHeight();

        nav.setLocation(x, y);
        nav.setVisible(true);

        // 4. Tutup otomatis jika user mengklik di luar dialog
        nav.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            public void windowGainedFocus(java.awt.event.WindowEvent evt) {}
            public void windowLostFocus(java.awt.event.WindowEvent evt) {
                nav.dispose();
            }
        });
    }//GEN-LAST:event_lblNotifMouseClicked

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(k_kelolaPeminjamanSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(k_kelolaPeminjamanSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(k_kelolaPeminjamanSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(k_kelolaPeminjamanSiswa.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new k_kelolaPeminjamanSiswa().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack3;
    private javax.swing.JButton btnCetakStruk;
    private javax.swing.JButton btnCheckBuku;
    private javax.swing.JButton btnDaftarBuku;
    private javax.swing.JButton btnDashboard;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnFilterBulan;
    private javax.swing.JButton btnFilterBulanDanTahun;
    private javax.swing.JButton btnFilterTahun;
    private javax.swing.JButton btnHapusBuku;
    private javax.swing.JButton btnKelolaPeminjaman;
    private javax.swing.JButton btnKelolaPengembalian;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnRiwayatSiswa;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnTambahBuku;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JComboBox<String> cmbBulan;
    private javax.swing.JComboBox<String> cmbTahun;
    private com.toedter.calendar.JDateChooser dateTanggalKembali;
    private com.toedter.calendar.JDateChooser dateTanggalPinjam;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel lblNotif;
    private javax.swing.JTable tblBuku;
    private javax.swing.JTable tblPeminjaman;
    private javax.swing.JTextField txtIdPeminjaman;
    private javax.swing.JTextField txtJudul;
    private javax.swing.JTextField txtKodeBuku;
    private javax.swing.JTextField txtNama;
    private javax.swing.JTextField txtNisn;
    // End of variables declaration//GEN-END:variables
}
