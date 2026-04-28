/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.concurrent.TimeUnit;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import koneksi.koneksi;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRResultSetDataSource;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.swing.JRViewer;

/**
 *
 * @author Dell
 */
public class j_kelolaPengembalianAdmin extends javax.swing.JFrame {

    /**
     * Creates new form peminjamans_page
     */
    public j_kelolaPengembalianAdmin() {
        this.setUndecorated(true);
        initComponents();
        setTableStyleReturn();
        loadReturnData();
        setTanggalHariIni();

        if (cmbTahun != null) {
            cmbTahun.removeAllItems();
            cmbTahun.addItem("Semua Tahun");

            // Tambahkan 5 tahun terakhir
            int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
            for (int i = currentYear; i >= currentYear - 5; i--) {
                cmbTahun.addItem(String.valueOf(i));
            }
        }
    }

    private DefaultTableModel model;
    ResultSet rs = null;
    PreparedStatement pst = null;
    
    private void setTableStyleReturn() {

    // STYLE tblReturn
    tblReturn.setOpaque(true);
    tblReturn.setBackground(java.awt.Color.WHITE);
    tblReturn.setForeground(java.awt.Color.BLACK);

    // ukuran baris
    tblReturn.setRowHeight(25);

    // font tabel
    tblReturn.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));

    // warna saat baris dipilih
    tblReturn.setSelectionBackground(new java.awt.Color(0,120,215));
    tblReturn.setSelectionForeground(java.awt.Color.WHITE);

    // garis tabel
    tblReturn.setShowGrid(true);
    tblReturn.setGridColor(new java.awt.Color(220,220,220));

    // header tabel
    tblReturn.getTableHeader().setBackground(new java.awt.Color(245,245,245));
    tblReturn.getTableHeader().setForeground(java.awt.Color.BLACK);
    tblReturn.getTableHeader().setFont(
        new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 12)
    );
}

    private void loadReturnData() {
        DefaultTableModel model = (DefaultTableModel) tblReturn.getModel();
        model.setRowCount(0);

        try {
            Connection conn = koneksi.KoneksiDB();

            String sql = "SELECT r.id AS return_id, "
                    + "b.id AS borrowing_id, "
                    + "s.nisn, "
                    + "s.name AS student_name, "
                    + "GROUP_CONCAT(DISTINCT bk.title SEPARATOR ', ') AS book_titles, "
                    + "b.returned_at AS due_date, " // Tenggat pengembalian
                    + "r.returned_at AS actual_return_date, " // Tanggal pengembalian sebenarnya
                    + "r.charge, "
                    + "r.status " // Tambahkan status
                    + "FROM returns r "
                    + "JOIN borrowings b ON r.borrowing_id = b.id "
                    + "JOIN students s ON b.student_nisn = s.nisn "
                    + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
                    + "JOIN book_items bi ON bd.book_code = bi.code " // PERBAIKAN: gunakan book_items
                    + "JOIN books bk ON bi.book_id = bk.id " // PERBAIKAN: hubungkan ke books melalui book_items
                    + "GROUP BY r.id, b.id, s.nisn, s.name, "
                    + "b.returned_at, r.returned_at, r.charge, r.status " // Tambahkan status di GROUP BY
                    + "ORDER BY r.returned_at DESC";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                int denda = rs.getInt("charge");
                String dendaFormatted = formatRupiah(denda);

                model.addRow(new Object[]{
                    rs.getInt("return_id"), //  ID Pengembalian
                    rs.getInt("borrowing_id"), // ID Peminjaman
                    rs.getString("nisn"), // NISN
                    rs.getString("student_name"), //  Nama Siswa
                    rs.getString("book_titles"), //  Judul Buku
                    rs.getDate("due_date"), //  Tenggat Pengembalian (dari borrowings)
                    rs.getDate("actual_return_date"), // Dikembalikan Pada (dari returns)
                    dendaFormatted, //  Denda
                    rs.getString("status"),});
            }

            // Tutup koneksi dan statement
            rs.close();
            st.close();
            conn.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            e.printStackTrace(); // Untuk debugging
        }
    }

    private void setTanggalHariIni() {
        java.util.Date tanggalSekarang = new java.util.Date();

        dateTanggalKembali.setDate(tanggalSekarang);
    }

    private void clearForm() {
        txtIdPengembalian.setText("");
        txtIdPeminjaman.setText("");
        txtNisn.setText("");
        txtNama.setText("");
        txtJudul.setText("");
        txtDenda.setText("");
        dateTenggatPinjam.setDate(null);
        dateTanggalKembali.setDate(null);
        loadReturnData();
        setTanggalHariIni();
    }

    private String formatRupiah(int nominal) {
        DecimalFormat df = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        DecimalFormatSymbols dfs = new DecimalFormatSymbols();

        dfs.setCurrencySymbol("Rp");
        dfs.setMonetaryDecimalSeparator(',');
        dfs.setGroupingSeparator('.');

        df.setDecimalFormatSymbols(dfs);
        df.setMaximumFractionDigits(0); // Menghilangkan ,00 di belakang

        return df.format(nominal);
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
        txtNisn = new javax.swing.JTextField();
        txtIdPeminjaman = new javax.swing.JTextField();
        txtJudul = new javax.swing.JTextField();
        txtIdPengembalian = new javax.swing.JTextField();
        dateTanggalKembali = new com.toedter.calendar.JDateChooser();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblReturn = new javax.swing.JTable();
        btnUpdate = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        btnDelete = new javax.swing.JButton();
        btnSave = new javax.swing.JButton();
        dateTenggatPinjam = new com.toedter.calendar.JDateChooser();
        btnCariKalender = new javax.swing.JButton();
        cmbTahun = new javax.swing.JComboBox<>();
        cmbBulan = new javax.swing.JComboBox<>();
        btnCetakLaporan = new javax.swing.JButton();
        btnPeminjaman = new javax.swing.JButton();
        txtNama = new javax.swing.JTextField();
        txtDenda = new javax.swing.JTextField();
        btnCheckDenda = new javax.swing.JButton();
        btnBack3 = new javax.swing.JButton();
        btnCetakStruk = new javax.swing.JButton();
        btnFilterHariIni = new javax.swing.JButton();
        btnCetakLaporanHariIni = new javax.swing.JButton();
        btnCheckPeminjaman = new javax.swing.JButton();
        btnKonfirmasi = new javax.swing.JButton();
        btnDashboard = new javax.swing.JButton();
        btnKelolaBuku = new javax.swing.JButton();
        btnKelolaSiswa = new javax.swing.JButton();
        btnKelolaPeminjaman = new javax.swing.JButton();
        btnKelolaPengembalian = new javax.swing.JButton();
        btnKelolaKategori = new javax.swing.JButton();
        btnKelolaPengunjung = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        lblNotifAdmin = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        txtNisn.setBackground(new java.awt.Color(0,0,0,0));
        txtNisn.setBorder(null);
        txtNisn.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        txtNisn.setEnabled(false);
        jPanel1.add(txtNisn, new org.netbeans.lib.awtextra.AbsoluteConstraints(1090, 280, 160, 20));

        txtIdPeminjaman.setBackground(new java.awt.Color(0,0,0,0));
        txtIdPeminjaman.setBorder(null);
        jPanel1.add(txtIdPeminjaman, new org.netbeans.lib.awtextra.AbsoluteConstraints(1090, 190, 160, 20));

        txtJudul.setBackground(new java.awt.Color(0,0,0,0));
        txtJudul.setBorder(null);
        txtJudul.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        txtJudul.setEnabled(false);
        jPanel1.add(txtJudul, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 370, 380, 30));

        txtIdPengembalian.setBackground(new java.awt.Color(0,0,0,0));
        txtIdPengembalian.setBorder(null);
        jPanel1.add(txtIdPengembalian, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 190, 190, 20));

        dateTanggalKembali.setEnabled(false);
        jPanel1.add(dateTanggalKembali, new org.netbeans.lib.awtextra.AbsoluteConstraints(850, 530, 410, 40));

        tblReturn.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "ID Pengembalian", "ID Peminjaman", "NISN Siswa", "Nama Siswa", "Judul Buku", "Tenggat Pengembalian", "Dikembalikan Pada", "Denda", "Status"
            }
        ));
        tblReturn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblReturnMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                tblReturnMouseEntered(evt);
            }
        });
        jScrollPane1.setViewportView(tblReturn);

        jPanel1.add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(310, 240, 500, 340));

        btnUpdate.setBackground(new java.awt.Color(0,0,0,0));
        btnUpdate.setBorder(null);
        btnUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUpdateActionPerformed(evt);
            }
        });
        jPanel1.add(btnUpdate, new org.netbeans.lib.awtextra.AbsoluteConstraints(960, 660, 100, 40));

        btnRefresh.setBackground(new java.awt.Color(0,0,0,0));
        btnRefresh.setBorder(null);
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });
        jPanel1.add(btnRefresh, new org.netbeans.lib.awtextra.AbsoluteConstraints(1070, 660, 110, 40));

        btnDelete.setBackground(new java.awt.Color(0,0,0,0));
        btnDelete.setBorder(null);
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });
        jPanel1.add(btnDelete, new org.netbeans.lib.awtextra.AbsoluteConstraints(1190, 660, 100, 40));

        btnSave.setBackground(new java.awt.Color(0,0,0,0));
        btnSave.setBorder(null);
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });
        jPanel1.add(btnSave, new org.netbeans.lib.awtextra.AbsoluteConstraints(850, 660, 90, 40));

        dateTenggatPinjam.setDateFormatString("d  MMM, yyyy");
        dateTenggatPinjam.setEnabled(false);
        jPanel1.add(dateTenggatPinjam, new org.netbeans.lib.awtextra.AbsoluteConstraints(850, 450, 410, 40));

        btnCariKalender.setBackground(new java.awt.Color(0,0,0,0));
        btnCariKalender.setBorder(null);
        btnCariKalender.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCariKalenderActionPerformed(evt);
            }
        });
        jPanel1.add(btnCariKalender, new org.netbeans.lib.awtextra.AbsoluteConstraints(780, 190, 40, 40));

        cmbTahun.setBackground(new java.awt.Color(0, 0, 0, 0));
        cmbTahun.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        cmbTahun.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Semua Tahun", "2024", "2025", "2026", "2027", "2028" }));
        cmbTahun.setBorder(null);
        jPanel1.add(cmbTahun, new org.netbeans.lib.awtextra.AbsoluteConstraints(650, 190, 120, 30));

        cmbBulan.setBackground(new java.awt.Color(0, 0, 0, 0));
        cmbBulan.setFont(new java.awt.Font("Tahoma", 1, 12)); // NOI18N
        cmbBulan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember" }));
        cmbBulan.setBorder(null);
        jPanel1.add(cmbBulan, new org.netbeans.lib.awtextra.AbsoluteConstraints(470, 190, 150, 30));

        btnCetakLaporan.setBackground(new java.awt.Color(0,0,0,0));
        btnCetakLaporan.setBorder(null);
        btnCetakLaporan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetakLaporanActionPerformed(evt);
            }
        });
        jPanel1.add(btnCetakLaporan, new org.netbeans.lib.awtextra.AbsoluteConstraints(640, 660, 170, 40));

        btnPeminjaman.setBackground(new java.awt.Color(0,0,0,0));
        btnPeminjaman.setBorder(null);
        btnPeminjaman.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPeminjamanActionPerformed(evt);
            }
        });
        jPanel1.add(btnPeminjaman, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 150, 510, 30));

        txtNama.setBackground(new java.awt.Color(0,0,0,0));
        txtNama.setBorder(null);
        txtNama.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        txtNama.setEnabled(false);
        jPanel1.add(txtNama, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 280, 190, 20));

        txtDenda.setBackground(new java.awt.Color(0,0,0,0));
        txtDenda.setBorder(null);
        txtDenda.setDisabledTextColor(new java.awt.Color(0, 0, 0));
        jPanel1.add(txtDenda, new org.netbeans.lib.awtextra.AbsoluteConstraints(870, 610, 380, 20));

        btnCheckDenda.setBackground(new java.awt.Color(0,0,0,0));
        btnCheckDenda.setBorder(null);
        btnCheckDenda.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCheckDendaActionPerformed(evt);
            }
        });
        jPanel1.add(btnCheckDenda, new org.netbeans.lib.awtextra.AbsoluteConstraints(1260, 590, 90, 50));

        btnBack3.setBackground(new java.awt.Color(0,0,0,0));
        btnBack3.setBorder(null);
        btnBack3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnBack3ActionPerformed(evt);
            }
        });
        jPanel1.add(btnBack3, new org.netbeans.lib.awtextra.AbsoluteConstraints(20, 690, 200, 60));

        btnCetakStruk.setBackground(new java.awt.Color(0,0,0,0));
        btnCetakStruk.setBorder(null);
        btnCetakStruk.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetakStrukActionPerformed(evt);
            }
        });
        jPanel1.add(btnCetakStruk, new org.netbeans.lib.awtextra.AbsoluteConstraints(490, 660, 140, 40));

        btnFilterHariIni.setBackground(new java.awt.Color(0,0,0,0));
        btnFilterHariIni.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterHariIniActionPerformed(evt);
            }
        });
        jPanel1.add(btnFilterHariIni, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 190, 150, 40));

        btnCetakLaporanHariIni.setBackground(new java.awt.Color(0,0,0,0));
        btnCetakLaporanHariIni.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetakLaporanHariIniActionPerformed(evt);
            }
        });
        jPanel1.add(btnCetakLaporanHariIni, new org.netbeans.lib.awtextra.AbsoluteConstraints(300, 660, 180, 40));

        btnCheckPeminjaman.setBackground(new java.awt.Color(0,0,0,0));
        btnCheckPeminjaman.setBorder(null);
        btnCheckPeminjaman.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCheckPeminjamanActionPerformed(evt);
            }
        });
        jPanel1.add(btnCheckPeminjaman, new org.netbeans.lib.awtextra.AbsoluteConstraints(1260, 180, 80, 50));

        btnKonfirmasi.setBackground(new java.awt.Color(0,0,0,0));
        btnKonfirmasi.setBorder(null);
        btnKonfirmasi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKonfirmasiActionPerformed(evt);
            }
        });
        jPanel1.add(btnKonfirmasi, new org.netbeans.lib.awtextra.AbsoluteConstraints(290, 590, 530, 50));

        btnDashboard.setBackground(new java.awt.Color(0,0,0,0));
        btnDashboard.setBorder(null);
        btnDashboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDashboardActionPerformed(evt);
            }
        });
        jPanel1.add(btnDashboard, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 140, 240, 40));

        btnKelolaBuku.setBackground(new java.awt.Color(0,0,0,0));
        btnKelolaBuku.setBorder(null);
        btnKelolaBuku.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKelolaBukuActionPerformed(evt);
            }
        });
        jPanel1.add(btnKelolaBuku, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 190, 240, 30));

        btnKelolaSiswa.setBackground(new java.awt.Color(0,0,0,0));
        btnKelolaSiswa.setBorder(null);
        btnKelolaSiswa.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKelolaSiswaActionPerformed(evt);
            }
        });
        jPanel1.add(btnKelolaSiswa, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 240, 240, 30));

        btnKelolaPeminjaman.setBackground(new java.awt.Color(0,0,0,0));
        btnKelolaPeminjaman.setBorder(null);
        btnKelolaPeminjaman.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKelolaPeminjamanActionPerformed(evt);
            }
        });
        jPanel1.add(btnKelolaPeminjaman, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 290, 240, 30));

        btnKelolaPengembalian.setBackground(new java.awt.Color(0,0,0,0));
        btnKelolaPengembalian.setBorder(null);
        btnKelolaPengembalian.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKelolaPengembalianActionPerformed(evt);
            }
        });
        jPanel1.add(btnKelolaPengembalian, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 330, 240, 40));

        btnKelolaKategori.setBackground(new java.awt.Color(0,0,0,0));
        btnKelolaKategori.setBorder(null);
        btnKelolaKategori.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKelolaKategoriActionPerformed(evt);
            }
        });
        jPanel1.add(btnKelolaKategori, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 380, 240, 30));

        btnKelolaPengunjung.setBackground(new java.awt.Color(0,0,0,0));
        btnKelolaPengunjung.setBorder(null);
        btnKelolaPengunjung.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnKelolaPengunjungActionPerformed(evt);
            }
        });
        jPanel1.add(btnKelolaPengunjung, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 420, 240, 40));

        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/image/KELOLA PENGEMBALIAN -ADMIN (2).png"))); // NOI18N
        jPanel1.add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, -1, -1));

        lblNotifAdmin.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lblNotifAdminMouseClicked(evt);
            }
        });
        jPanel1.add(lblNotifAdmin, new org.netbeans.lib.awtextra.AbsoluteConstraints(1280, 10, 70, 60));

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

    private void tblReturnMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblReturnMouseClicked
        // TODO add your handling code here:
        int row = tblReturn.getSelectedRow();
        if (row == -1) {
            return;
        }

        txtIdPengembalian.setText(tblReturn.getValueAt(row, 0).toString());
        txtIdPeminjaman.setText(tblReturn.getValueAt(row, 1).toString());
        txtNisn.setText(tblReturn.getValueAt(row, 2).toString());
        txtNama.setText(tblReturn.getValueAt(row, 3).toString());
        txtJudul.setText(tblReturn.getValueAt(row, 4).toString());
        dateTenggatPinjam.setDate((java.sql.Date) tblReturn.getValueAt(row, 5));
        dateTanggalKembali.setDate((java.sql.Date) tblReturn.getValueAt(row, 6));
        txtDenda.setText(tblReturn.getValueAt(row, 7).toString());
    }//GEN-LAST:event_tblReturnMouseClicked

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        // TODO add your handling code here:
        String borrowingId = txtIdPeminjaman.getText().trim();
        String dendaText = txtDenda.getText()
                .replace("Rp", "").replace(".", "").replace(",", "").trim();
        int charge = Integer.parseInt(dendaText);
        java.util.Date returnDate = dateTanggalKembali.getDate();

        if (borrowingId.isEmpty() || returnDate == null) {
            JOptionPane.showMessageDialog(this, "Data belum lengkap!");
            return;
        }

        try {
            Connection conn = koneksi.KoneksiDB();

            String sqlReturn = "INSERT INTO returns (borrowing_id, returned_at, charge, status) VALUES (?, ?, ?, 'process')";

            PreparedStatement pstReturn = conn.prepareStatement(sqlReturn);
            pstReturn.setInt(1, Integer.parseInt(borrowingId));
            pstReturn.setDate(2, new java.sql.Date(returnDate.getTime()));
            pstReturn.setInt(3, charge);

            int rowsAffected = pstReturn.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(this, "Pengembalian berhasil disimpan, Silahkan ke admin untuk konfirmasi");
                loadReturnData(); // Refresh tabel untuk melihat status "Dalam Proses"
                clearForm();
            }

            pstReturn.close();
            conn.close();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void tblReturnMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblReturnMouseEntered
        // TODO add your handling code here:
    }//GEN-LAST:event_tblReturnMouseEntered

    private void btnUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUpdateActionPerformed
        // TODO add your handling code here:
        String returnId = txtIdPengembalian.getText().trim(); // Gunakan ID Pengembalian
        java.util.Date tglKembali = dateTanggalKembali.getDate();

        if (returnId.isEmpty() || tglKembali == null) {
            JOptionPane.showMessageDialog(this, "Pilih data pengembalian yang akan diubah!");
            return;
        }

        try {
            Connection conn = koneksi.KoneksiDB();

            // Parsing denda dari textfield
            int charge = Integer.parseInt(txtDenda.getText().replace("Rp", "").replace(".", "").trim());

            // Update HANYA JIKA status masih 'process'
            String sql = "UPDATE returns SET returned_at = ?, charge = ? WHERE id = ? AND status = 'process'";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setDate(1, new java.sql.Date(tglKembali.getTime()));
            pst.setInt(2, charge);
            pst.setInt(3, Integer.parseInt(returnId));

            int affected = pst.executeUpdate();

            if (affected > 0) {
                JOptionPane.showMessageDialog(this, "Data berhasil diperbarui!");
                loadReturnData();
                clearForm();
            } else {
                // Jika affected 0, berarti ID salah ATAU status sudah 'done'
                JOptionPane.showMessageDialog(this, "Gagal update! Data tidak ditemukan atau status sudah 'Selesai'.");
            }
            conn.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Kesalahan: " + e.getMessage());
        }
    }//GEN-LAST:event_btnUpdateActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        // TODO add your handling code here:
        clearForm();
        setTanggalHariIni();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        // TODO add your handling code here:
        String returnId = txtIdPengembalian.getText().trim();

        if (returnId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data yang akan dihapus!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "Hapus data pengembalian ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Connection conn = koneksi.KoneksiDB();

                // Delete HANYA JIKA status masih 'process'
                String sql = "DELETE FROM returns WHERE id = ? AND status = 'process'";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setInt(1, Integer.parseInt(returnId));

                int affected = pst.executeUpdate();

                if (affected > 0) {
                    JOptionPane.showMessageDialog(this, "Data berhasil dihapus!");
                    loadReturnData();
                    clearForm();
                } else {
                    JOptionPane.showMessageDialog(this, "Gagal hapus! Data sudah berstatus 'Selesai'.");
                }
                conn.close();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Kesalahan: " + e.getMessage());
            }
        }
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnCariKalenderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCariKalenderActionPerformed
        // TODO add your handling code here:
        DefaultTableModel model = (DefaultTableModel) tblReturn.getModel();
        model.setRowCount(0);

        int bulan = cmbBulan.getSelectedIndex(); // 0 = Semua Bulan
        String selectedTahun = cmbTahun.getSelectedItem().toString();
        int tahun = selectedTahun.equals("Semua") ? 0 : Integer.parseInt(selectedTahun);

        if (bulan == 0 && tahun == 0) {
            loadReturnData();
            return;
        }

        // Base SQL (Sesuaikan dengan struktur JOIN yang benar)
        StringBuilder sql = new StringBuilder(
                "SELECT r.id AS return_id, b.id AS borrowing_id, s.nisn, s.name AS student_name, "
                + "GROUP_CONCAT(DISTINCT bk.title SEPARATOR ', ') AS book_titles, "
                + "b.returned_at AS due_date, r.returned_at AS actual_return_date, r.charge, r.status "
                + "FROM returns r "
                + "JOIN borrowings b ON r.borrowing_id = b.id "
                + "JOIN students s ON b.student_nisn = s.nisn "
                + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
                + "JOIN book_items bi ON bd.book_code = bi.code "
                + // Jembatan ke book_items
                "JOIN books bk ON bi.book_id = bk.id "
                + "WHERE 1=1 "
        );

        // Tambah kondisi filter secara dinamis
        if (bulan != 0) {
            sql.append("AND MONTH(r.returned_at) = ? ");
        }
        if (tahun != 0) {
            sql.append("AND YEAR(r.returned_at) = ? ");
        }

        sql.append("GROUP BY r.id ORDER BY r.returned_at DESC");

        try (Connection conn = koneksi.KoneksiDB();
                PreparedStatement pst = conn.prepareStatement(sql.toString())) {

            int paramIdx = 1;
            if (bulan != 0) {
                pst.setInt(paramIdx++, bulan);
            }
            if (tahun != 0) {
                pst.setInt(paramIdx++, tahun);
            }

            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
//                String statusIndo = rs.getString("status").equals("process") ? "Dalam Proses" : "Selesai";

                model.addRow(new Object[]{
                    rs.getInt("return_id"),
                    rs.getInt("borrowing_id"),
                    rs.getString("nisn"),
                    rs.getString("student_name"),
                    rs.getString("book_titles"),
                    rs.getDate("due_date"),
                    rs.getDate("actual_return_date"),
                    formatRupiah(rs.getInt("charge")), // Gunakan formatRupiah agar konsisten
                    //                    statusIndo
                    rs.getString("status")
                });
            }

            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Data pengembalian tidak ditemukan pada periode ini.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error filter: " + e.getMessage());
        }
    }//GEN-LAST:event_btnCariKalenderActionPerformed

    private void btnPeminjamanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPeminjamanActionPerformed
        // TODO add your handling code here:
        j_daftarPeminjamanAdmin obj = new j_daftarPeminjamanAdmin();
        obj.setVisible(true);
    }//GEN-LAST:event_btnPeminjamanActionPerformed

    private void btnCheckDendaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCheckDendaActionPerformed
        // TODO add your handling code here:
        java.util.Date tglPinjam = dateTenggatPinjam.getDate();
        java.util.Date tglKembali = dateTanggalKembali.getDate();

        if (tglPinjam == null || tglKembali == null) {
            JOptionPane.showMessageDialog(this, "Tanggal pinjam dan tanggal kembali harus diisi!");
            return;
        }

        // hitung selisih hari
        long selisihMillis = tglKembali.getTime() - tglPinjam.getTime();
        long selisihHari = TimeUnit.MILLISECONDS.toDays(selisihMillis);

        // denda per hari
        int dendaPerHari = 5000;
        long totalDenda = 0;

        if (selisihHari > 0) {
            totalDenda = selisihHari * dendaPerHari;
        }

        // format rupiah (Rp. x.xxx)
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');

        DecimalFormat rupiah = new DecimalFormat("Rp#,##0", symbols);

        txtDenda.setText(rupiah.format(totalDenda));
    }//GEN-LAST:event_btnCheckDendaActionPerformed

    private void btnBack3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnBack3ActionPerformed
        // TODO add your handling code here:
        this.dispose();
    }//GEN-LAST:event_btnBack3ActionPerformed

    private void btnCetakStrukActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakStrukActionPerformed
        // TODO add your handling code here:
        //  Validasi Input: Cek apakah ID sudah diisi
        if (txtIdPengembalian.getText().trim().isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(null, "Masukkan ID Pengembalian terlebih dahulu!");
            return;
        }

        //  Ambil ID dari TextField
        int idPengembalian;
        try {
            idPengembalian = Integer.parseInt(txtIdPengembalian.getText());
        } catch (NumberFormatException e) {
            javax.swing.JOptionPane.showMessageDialog(null, "ID Pengembalian harus berupa angka!");
            return;
        }

        //  Query SQL (Sesuai dengan struktur JRXML)
        String sql = "SELECT "
                + "students.`nisn` AS students_nisn, "
                + "students.`name` AS students_name, "
                + "students.`password` AS students_password, "
                + "students.`classroom` AS students_classroom, "
                + "students.`phone_number` AS students_phone_number, "
                + "students.`address` AS students_address, "
                + "returns.`id` AS returns_id, "
                + "returns.`borrowing_id` AS returns_borrowing_id, "
                + "returns.`returned_at` AS returns_returned_at, "
                + "returns.`charge` AS returns_charge, "
                + "returns.`status` AS returns_status, "
                + "returns.`created_at` AS returns_created_at, "
                + "categories.`id` AS categories_id, "
                + "categories.`name` AS categories_name, "
                + "borrowings.`id` AS borrowings_id, "
                + "borrowings.`student_nisn` AS borrowings_student_nisn, "
                + "DATE_FORMAT(borrowings.`borrowed_at`, '%d %M %Y') AS borrowings_borrowed_at, "
                + "DATE_FORMAT(borrowings.`returned_at`, '%d %M %Y') AS borrowings_returned_at, "
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
                + "INNER JOIN `returns` returns ON borrowings.`id` = returns.`borrowing_id` "
                + "INNER JOIN `borrowing_details` borrowing_details ON borrowings.`id` = borrowing_details.`borrowing_id` "
                + "INNER JOIN `book_items` book_items ON borrowing_details.`book_code` = book_items.`code` "
                + "INNER JOIN `books` books ON book_items.`book_id` = books.`id` "
                + "INNER JOIN `categories` categories ON books.`category_id` = categories.`id`, "
                + "`admins` admins "
                + "WHERE returns.`id` = ?";

        try {
            java.sql.Connection conn = (java.sql.Connection) koneksi.KoneksiDB();
            java.sql.PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, idPengembalian);
            java.sql.ResultSet rs = pst.executeQuery();

            //  Cek apakah data ditemukan
            if (!rs.isBeforeFirst()) {
                javax.swing.JOptionPane.showMessageDialog(null, "Data pengembalian dengan ID " + idPengembalian + " tidak ditemukan!");
                return;
            }

            //  Konfigurasi Path JRXML
            // Sesuaikan dengan letak file .jrxml di project Anda
            String reportPath = "src/report/c_strukPengembalian.jrxml";

            // Cetak Laporan
            net.sf.jasperreports.engine.JRResultSetDataSource jrRS = new net.sf.jasperreports.engine.JRResultSetDataSource(rs);
            net.sf.jasperreports.engine.JasperReport jasperReport = net.sf.jasperreports.engine.JasperCompileManager.compileReport(reportPath);
            net.sf.jasperreports.engine.JasperPrint jasperPrint = net.sf.jasperreports.engine.JasperFillManager.fillReport(jasperReport, null, jrRS);

            // Menampilkan Laporan di JDialog
            net.sf.jasperreports.swing.JRViewer aViewer = new net.sf.jasperreports.swing.JRViewer(jasperPrint);
            javax.swing.JDialog viewer = new javax.swing.JDialog();
            viewer.setTitle("Struk Pengembalian Buku");
            viewer.setAlwaysOnTop(true);
            viewer.getContentPane().add(aViewer);

            // Atur ukuran jendela laporan
            java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            viewer.setBounds(0, 0, screenSize.width, screenSize.height);
            viewer.setVisible(true);

        } catch (java.sql.SQLException e) {
            javax.swing.JOptionPane.showMessageDialog(null, "Gagal mengambil data: " + e.getMessage());
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(null, "Gagal menampilkan laporan: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnCetakStrukActionPerformed

    private void btnCetakLaporanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakLaporanActionPerformed
        // TODO add your handling code here:
        int selectedMonth = cmbBulan.getSelectedIndex(); // 0 = Semua bulan, 1 = Januari, 2 = Februari, dst
        int selectedYear;

        try {
            selectedYear = Integer.parseInt(cmbTahun.getSelectedItem().toString());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Tahun tidak valid! Pastikan telah memilih tahun");
            return;
        }

        // Format periode teks untuk laporan
        String periodeText = "";
        if (selectedMonth == 0) {
            periodeText = "Tahun " + selectedYear;
        } else {
            // Konversi index ke nama bulan (index 0 = Semua, index 1 = Januari)
            String[] bulanArray = {"", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
            if (selectedMonth >= 1 && selectedMonth <= 12) {
                periodeText = bulanArray[selectedMonth] + " " + selectedYear;
            } else {
                periodeText = "Periode Tidak Valid";
            }
        }

        // Tambahkan informasi dicetak oleh admin
        String adminName = session.session_admin.nameAdmin;
        if (adminName != null && !adminName.trim().isEmpty()) {
            periodeText += " | Dicetak oleh: " + adminName;
        }

        // Path ke file report jrxml
        String reportPath = "src/report/d_laporanPengembalian.jrxml";

        // Query berdasarkan periode yang dipilih (sesuai dengan struktur JRXML)
        String sql = "SELECT "
                + "admins.`id` AS admins_id, "
                + "admins.`name` AS admins_name, "
                + "admins.`username` AS admins_username, "
                + "admins.`password` AS admins_password, "
                + "book_items.`code` AS book_items_code, "
                + "book_items.`book_id` AS book_items_book_id, "
                + "book_items.`status` AS book_items_status, "
                + "books.`id` AS books_id, "
                + "books.`title` AS books_title, "
                + "books.`author` AS books_author, "
                + "books.`publisher` AS books_publisher, "
                + "books.`origin` AS books_origin, "
                + "books.`total_pages` AS books_total_pages, "
                + "books.`cover` AS books_cover, "
                + "books.`category_id` AS books_category_id, "
                + "borrowing_details.`id` AS borrowing_details_id, "
                + "borrowing_details.`borrowing_id` AS borrowing_details_borrowing_id, "
                + "borrowing_details.`book_code` AS borrowing_details_book_code, "
                + "borrowings.`id` AS borrowings_id, "
                + "borrowings.`student_nisn` AS borrowings_student_nisn, "
                + "DATE_FORMAT(borrowings.`borrowed_at`, '%d %M %Y') AS borrowings_borrowed_at, "
                + "DATE_FORMAT(borrowings.`returned_at`, '%d %M %Y') AS borrowings_returned_at, "
                + "borrowings.`status` AS borrowings_status, "
                + "borrowings.`created_at` AS borrowings_created_at, "
                + "categories.`id` AS categories_id, "
                + "categories.`name` AS categories_name, "
                + "returns.`id` AS returns_id, "
                + "returns.`borrowing_id` AS returns_borrowing_id, "
                + "returns.`returned_at` AS returns_returned_at, "
                + "returns.`charge` AS returns_charge, "
                + "returns.`status` AS returns_status, "
                + "returns.`created_at` AS returns_created_at, "
                + "students.`nisn` AS students_nisn, "
                + "students.`name` AS students_name, "
                + "students.`password` AS students_password, "
                + "students.`classroom` AS students_classroom, "
                + "students.`phone_number` AS students_phone_number, "
                + "students.`address` AS students_address "
                + "FROM `books` books "
                + "INNER JOIN `book_items` book_items ON books.`id` = book_items.`book_id` "
                + "INNER JOIN `borrowing_details` borrowing_details ON book_items.`code` = borrowing_details.`book_code` "
                + "INNER JOIN `borrowings` borrowings ON borrowing_details.`borrowing_id` = borrowings.`id` "
                + "INNER JOIN `returns` returns ON borrowings.`id` = returns.`borrowing_id` "
                + "INNER JOIN `students` students ON borrowings.`student_nisn` = students.`nisn` "
                + "INNER JOIN `categories` categories ON books.`category_id` = categories.`id`, "
                + "`admins` admins "
                + "WHERE 1=1";

        // Tambahkan filter bulan jika bukan "Semua bulan"
        if (selectedMonth != 0) {
            sql += " AND MONTH(returns.`returned_at`) = ?";
        }

        // Tambahkan filter tahun
        sql += " AND YEAR(returns.`returned_at`) = ?";

        // Tambahkan ORDER BY untuk mengurutkan berdasarkan tanggal pengembalian
        sql += " ORDER BY returns.`returned_at` DESC";

        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rp = null;

        try {
            // Koneksi database
            conn = koneksi.KoneksiDB();
            pst = conn.prepareStatement(sql);

            // Set parameter berdasarkan filter
            int paramIndex = 1;
            if (selectedMonth != 0) {
                pst.setInt(paramIndex++, selectedMonth); // Bulan
            }
            pst.setInt(paramIndex, selectedYear); // Tahun

            // Eksekusi query
            rp = pst.executeQuery();

            // Cek apakah ada data
            if (!rp.isBeforeFirst()) {
                JOptionPane.showMessageDialog(null, "Tidak ada data pengembalian untuk periode " + periodeText);
                return;
            }

            // Siapkan parameter untuk report
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PERIODE_TEXT", periodeText);

            // Menampilkan laporan
            JRResultSetDataSource jrRS = new JRResultSetDataSource(rp);
            JasperReport jasperReport = JasperCompileManager.compileReport(reportPath);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, jrRS);

            // Menampilkan laporan
            JRViewer aViewer = new JRViewer(jasperPrint);
            JDialog viewer = new JDialog();
            viewer.setTitle("Laporan Pengembalian - " + periodeText);
            viewer.setAlwaysOnTop(true);
            viewer.getContentPane().add(aViewer);

            // Set ukuran dialog
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            viewer.setBounds(0, 0, screenSize.width, screenSize.height);
            viewer.setLocationRelativeTo(null); // Center dialog
            viewer.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal mengambil data laporan: " + e.getMessage());
            e.printStackTrace();
        } catch (JRException e) {
            JOptionPane.showMessageDialog(null, "Gagal mengkompilasi laporan: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Gagal menampilkan laporan: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Tutup koneksi dan resources
            try {
                if (rp != null) {
                    rp.close();
                }
                if (pst != null) {
                    pst.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_btnCetakLaporanActionPerformed

    private void btnFilterHariIniActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterHariIniActionPerformed
        // TODO add your handling code here:
        DefaultTableModel model = (DefaultTableModel) tblReturn.getModel();
        model.setRowCount(0);

        try (Connection conn = koneksi.KoneksiDB()) {
            // Query SQL menggunakan CURDATE() untuk mendapatkan data pengembalian hari ini
            String sql = "SELECT r.id AS return_id, "
                    + "b.id AS borrowing_id, "
                    + "s.nisn, "
                    + "s.name AS student_name, "
                    + "GROUP_CONCAT(DISTINCT bk.title SEPARATOR ', ') AS book_titles, "
                    + "b.returned_at AS due_date, " // Tenggat pengembalian
                    + "r.returned_at AS actual_return_date, " // Tanggal pengembalian sebenarnya
                    + "r.charge, "
                    + "r.status " // Status
                    + "FROM returns r "
                    + "JOIN borrowings b ON r.borrowing_id = b.id "
                    + "JOIN students s ON b.student_nisn = s.nisn "
                    + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
                    + "JOIN book_items bi ON bd.book_code = bi.code "
                    + "JOIN books bk ON bi.book_id = bk.id "
                    + "WHERE DATE(r.returned_at) = CURDATE() " // Filter tanggal pengembalian hari ini
                    + "GROUP BY r.id, b.id, s.nisn, s.name, "
                    + "b.returned_at, r.returned_at, r.charge, r.status "
                    + "ORDER BY r.returned_at DESC";

            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);

            while (rs.next()) {
                int denda = rs.getInt("charge");
                String dendaFormatted = formatRupiah(denda);

                model.addRow(new Object[]{
                    rs.getInt("return_id"), // 1. ID Pengembalian
                    rs.getInt("borrowing_id"), // 2. ID Peminjaman
                    rs.getString("nisn"), // 3. NISN
                    rs.getString("student_name"), // 4. Nama Siswa
                    rs.getString("book_titles"), // 5. Judul Buku
                    rs.getDate("due_date"), // 6. Tenggat Pengembalian (dari borrowings)
                    rs.getDate("actual_return_date"), // 7. Dikembalikan Pada (dari returns)
                    dendaFormatted, // 8. Denda
                    rs.getString("status") // 9. Status
                });
            }

            // Tutup koneksi dan statement
            rs.close();
            st.close();

            // Opsional: Beri pesan jika data tidak ditemukan
            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "Tidak ada data pengembalian untuk hari ini.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal filter data hari ini: " + e.getMessage());
            e.printStackTrace();
        }
    }//GEN-LAST:event_btnFilterHariIniActionPerformed

    private void btnCetakLaporanHariIniActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakLaporanHariIniActionPerformed
        // TODO add your handling code here:
        // Format periode teks untuk laporan hari ini
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMMM yyyy");
        String tanggalHariIni = sdf.format(new java.util.Date());
        String periodeText = "Hari Ini (" + tanggalHariIni + ")";

        // Tambahkan informasi dicetak oleh admin
        String adminName = session.session_admin.nameAdmin;
        if (adminName != null && !adminName.trim().isEmpty()) {
            periodeText += " | Dicetak oleh: " + adminName;
        }

        // Path ke file report jrxml
        String reportPath = "src/report/d_laporanPengembalian.jrxml";

        // Query khusus untuk hari ini menggunakan CURDATE() (sesuai struktur JRXML pengembalian)
        String sql = "SELECT "
                + "admins.`id` AS admins_id, "
                + "admins.`name` AS admins_name, "
                + "admins.`password` AS admins_password, "
                + "book_items.`code` AS book_items_code, "
                + "book_items.`book_id` AS book_items_book_id, "
                + "book_items.`status` AS book_items_status, "
                + "books.`id` AS books_id, "
                + "books.`title` AS books_title, "
                + "books.`author` AS books_author, "
                + "books.`publisher` AS books_publisher, "
                + "books.`origin` AS books_origin, "
                + "books.`total_pages` AS books_total_pages, "
                + "books.`cover` AS books_cover, "
                + "books.`category_id` AS books_category_id, "
                + "borrowing_details.`id` AS borrowing_details_id, "
                + "borrowing_details.`borrowing_id` AS borrowing_details_borrowing_id, "
                + "borrowing_details.`book_code` AS borrowing_details_book_code, "
                + "borrowings.`id` AS borrowings_id, "
                + "borrowings.`student_nisn` AS borrowings_student_nisn, "
                + "DATE_FORMAT(borrowings.`borrowed_at`, '%d %M %Y') AS borrowings_borrowed_at, "
                + "DATE_FORMAT(borrowings.`returned_at`, '%d %M %Y') AS borrowings_returned_at, "
                + "borrowings.`status` AS borrowings_status, "
                + "borrowings.`created_at` AS borrowings_created_at, "
                + "categories.`id` AS categories_id, "
                + "categories.`name` AS categories_name, "
                + "returns.`id` AS returns_id, "
                + "returns.`borrowing_id` AS returns_borrowing_id, "
                + "returns.`returned_at` AS returns_returned_at, "
                + "returns.`charge` AS returns_charge, "
                + "returns.`status` AS returns_status, "
                + "returns.`created_at` AS returns_created_at, "
                + "students.`nisn` AS students_nisn, "
                + "students.`name` AS students_name, "
                + "students.`password` AS students_password, "
                + "students.`classroom` AS students_classroom, "
                + "students.`phone_number` AS students_phone_number, "
                + "students.`address` AS students_address "
                + "FROM `books` books "
                + "INNER JOIN `book_items` book_items ON books.`id` = book_items.`book_id` "
                + "INNER JOIN `borrowing_details` borrowing_details ON book_items.`code` = borrowing_details.`book_code` "
                + "INNER JOIN `borrowings` borrowings ON borrowing_details.`borrowing_id` = borrowings.`id` "
                + "INNER JOIN `returns` returns ON borrowings.`id` = returns.`borrowing_id` "
                + "INNER JOIN `students` students ON borrowings.`student_nisn` = students.`nisn` "
                + "INNER JOIN `categories` categories ON books.`category_id` = categories.`id`, "
                + "`admins` admins "
                + "WHERE DATE(returns.`returned_at`) = CURDATE() " // Filter pengembalian hari ini
                + "ORDER BY returns.`returned_at` DESC";

        Connection conn = null;
        PreparedStatement pst = null;
        ResultSet rp = null;

        try {
            conn = koneksi.KoneksiDB();
            pst = conn.prepareStatement(sql);
            rp = pst.executeQuery();

            // Cek apakah ada data
            if (!rp.isBeforeFirst()) {
                JOptionPane.showMessageDialog(null, "Tidak ada data pengembalian untuk hari ini.");
                return;
            }

            // Siapkan parameter untuk report
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("PERIODE_TEXT", periodeText);

            // Proses JasperReport
            JRResultSetDataSource jrRS = new JRResultSetDataSource(rp);
            JasperReport jasperReport = JasperCompileManager.compileReport(reportPath);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, jrRS);

            // Menampilkan laporan dalam JDialog
            JRViewer aViewer = new JRViewer(jasperPrint);
            JDialog viewer = new JDialog();
            viewer.setTitle("Laporan Pengembalian Hari Ini - " + tanggalHariIni);
            viewer.setAlwaysOnTop(true);
            viewer.getContentPane().add(aViewer);

            // Set ukuran dialog (Fullscreen)
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            viewer.setBounds(0, 0, screenSize.width, screenSize.height);
            viewer.setLocationRelativeTo(null);
            viewer.setVisible(true);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Gagal menampilkan laporan hari ini: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Tutup resource
            try {
                if (rp != null) {
                    rp.close();
                }
                if (pst != null) {
                    pst.close();
                }
                if (conn != null) {
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_btnCetakLaporanHariIniActionPerformed

    private void btnCheckPeminjamanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCheckPeminjamanActionPerformed
        // TODO add your handling code here:
        String input = txtIdPeminjaman.getText().trim();
        if (input.isEmpty()) {
            return;
        }

        try {
            Connection conn = koneksi.KoneksiDB();

            // Validasi: Cek apakah ID sudah ada di tabel returns
            String cekReturnSql = "SELECT COUNT(*) FROM returns WHERE borrowing_id = ?";
            PreparedStatement cekPst = conn.prepareStatement(cekReturnSql);
            cekPst.setInt(1, Integer.parseInt(input));
            ResultSet cekRs = cekPst.executeQuery();

            if (cekRs.next() && cekRs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this,
                    "ID Peminjaman " + input + " sudah dikembalikan!\nTidak dapat memproses pengembalian ulang.",
                    "Validasi Gagal",
                    JOptionPane.WARNING_MESSAGE);
                conn.close();
                return;
            }

            // Cek apakah ID Peminjaman ada di tabel borrowings
            String cekExistSql = "SELECT status FROM borrowings WHERE id = ?";
            PreparedStatement cekExistPst = conn.prepareStatement(cekExistSql);
            cekExistPst.setInt(1, Integer.parseInt(input));
            ResultSet rsExist = cekExistPst.executeQuery();

            if (!rsExist.next()) {
                JOptionPane.showMessageDialog(this,
                    "ID Peminjaman " + input + " tidak ditemukan dalam sistem!",
                    "Data Tidak Ditemukan",
                    JOptionPane.ERROR_MESSAGE);
                conn.close();
                return;
            }

            // Cek status peminjaman
            String status = rsExist.getString("status");
            if (!"approved".equals(status)) {
                JOptionPane.showMessageDialog(this,
                    "ID Peminjaman " + input + " statusnya: " + status + "\nHanya peminjaman dengan status 'approved' yang dapat diproses!",
                    "Status Belum Disetujui",
                    JOptionPane.WARNING_MESSAGE);
                conn.close();
                return;
            }

            // Query untuk mengambil detail peminjaman yang sudah approved
            String sql = "SELECT s.nisn, s.name, GROUP_CONCAT(bk.title SEPARATOR ', ') AS books, b.returned_at "
            + "FROM borrowings b "
            + "JOIN students s ON b.student_nisn = s.nisn "
            + "JOIN borrowing_details bd ON b.id = bd.borrowing_id "
            + "JOIN book_items bi ON bd.book_code = bi.code "
            + "JOIN books bk ON bi.book_id = bk.id "
            + "WHERE b.id = ? AND b.status = 'approved' "
            + "GROUP BY b.id";

            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, Integer.parseInt(input));
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                // Isi Field Utama
                txtNisn.setText(rs.getString("nisn"));
                txtNama.setText(rs.getString("name"));
                txtJudul.setText(rs.getString("books"));

                // Set Tanggal Tenggat
                java.sql.Date dueDate = rs.getDate("returned_at");
                dateTenggatPinjam.setDate(dueDate);
            }

            conn.close();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }//GEN-LAST:event_btnCheckPeminjamanActionPerformed

    private void btnKonfirmasiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKonfirmasiActionPerformed
        // TODO add your handling code here:
        int selectedRow = tblReturn.getSelectedRow();

        // Cek apakah ada baris yang dipilih
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Pilih data pengembalian yang akan dikonfirmasi!");
            return;
        }

        // Ambil ID dari tabel
        String returnId = tblReturn.getValueAt(selectedRow, 0).toString();   // index 0 = id pengembalian
        String borrowingId = tblReturn.getValueAt(selectedRow, 1).toString(); // index 1 = id peminjaman

        int confirm = JOptionPane.showConfirmDialog(this,
            "Konfirmasi pengembalian? \nBuku akan tersedia dan status peminjaman akan ditutup.",
            "Konfirmasi",
            JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            Connection conn = null;
            try {
                conn = koneksi.KoneksiDB();
                conn.setAutoCommit(false); 

                
                String sqlReturn = "UPDATE returns SET status = 'done' WHERE id = ? AND status = 'process'";
                PreparedStatement pstReturn = conn.prepareStatement(sqlReturn);
                pstReturn.setInt(1, Integer.parseInt(returnId));
                int returnUpdated = pstReturn.executeUpdate();

                if (returnUpdated > 0) {

                    
                    String sqlBooks = "UPDATE book_items bi "
                    + "JOIN borrowing_details bd ON bi.code = bd.book_code "
                    + "SET bi.status = 'available' "
                    + "WHERE bd.borrowing_id = ?";
                    PreparedStatement pstBooks = conn.prepareStatement(sqlBooks);
                    pstBooks.setInt(1, Integer.parseInt(borrowingId));
                    pstBooks.executeUpdate();

                    // Update borrowings → done
                    String sqlBorrow = "UPDATE borrowings SET status = 'done' WHERE id = ? AND status = 'approved'";
                    PreparedStatement pstBorrow = conn.prepareStatement(sqlBorrow);
                    pstBorrow.setInt(1, Integer.parseInt(borrowingId));
                    pstBorrow.executeUpdate();

                    conn.commit(); // Simpan semua perubahan

                    JOptionPane.showMessageDialog(this, "Berhasil! Data peminjaman telah diselesaikan.");

                    loadReturnData();
                    clearForm();

                } else {
                    JOptionPane.showMessageDialog(this, "Data tidak ditemukan atau sudah diproses sebelumnya.");
                }

            } catch (Exception e) {
                try {
                    if (conn != null) {
                        conn.rollback();
                    }
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                JOptionPane.showMessageDialog(this, "Gagal Konfirmasi: " + e.getMessage());
            } finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }//GEN-LAST:event_btnKonfirmasiActionPerformed

    private void btnDashboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDashboardActionPerformed
        d_dashboardAdmin obj = new d_dashboardAdmin();
        obj.setVisible(true);
    }//GEN-LAST:event_btnDashboardActionPerformed

    private void btnKelolaBukuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKelolaBukuActionPerformed
        // TODO add your handling code here:
        j_kelolaBukuAdmin obj = new j_kelolaBukuAdmin();
        obj.setVisible(true);
    }//GEN-LAST:event_btnKelolaBukuActionPerformed

    private void btnKelolaSiswaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKelolaSiswaActionPerformed
        j_kelolaSiswa obj = new j_kelolaSiswa();
        obj.setVisible(true);
    }//GEN-LAST:event_btnKelolaSiswaActionPerformed

    private void btnKelolaPeminjamanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKelolaPeminjamanActionPerformed
        // TODO add your handling code here:
        j_kelolaPeminjamanAdmin obj = new j_kelolaPeminjamanAdmin();
        obj.setVisible(true);
    }//GEN-LAST:event_btnKelolaPeminjamanActionPerformed

    private void btnKelolaPengembalianActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKelolaPengembalianActionPerformed
        // TODO add your handling code here:
        j_kelolaPengembalianAdmin obj = new j_kelolaPengembalianAdmin();
        obj.setVisible(true);
    }//GEN-LAST:event_btnKelolaPengembalianActionPerformed

    private void btnKelolaKategoriActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKelolaKategoriActionPerformed
        j_kelolaKategoriAdmin obj = new j_kelolaKategoriAdmin();
        obj.setVisible(true);
    }//GEN-LAST:event_btnKelolaKategoriActionPerformed

    private void btnKelolaPengunjungActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnKelolaPengunjungActionPerformed
        // TODO add your handling code here:
        j_kelolaPengunjungAdmin obj = new j_kelolaPengunjungAdmin();
        obj.setVisible(true);
    }//GEN-LAST:event_btnKelolaPengunjungActionPerformed

    private void lblNotifAdminMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lblNotifAdminMouseClicked
        // Memanggil JDialog Notifikasi Admin
        NotifikasiAdmin na = new NotifikasiAdmin(new javax.swing.JFrame(), false);

        // Mengatur posisi di bawah icon lonceng admin
        int x = lblNotifAdmin.getLocationOnScreen().x - 350;
        int y = lblNotifAdmin.getLocationOnScreen().y + lblNotifAdmin.getHeight();

        na.setLocation(x, y);
        na.setVisible(true);

        // Menutup otomatis jika Admin mengklik area lain
        na.addWindowFocusListener(new java.awt.event.WindowFocusListener() {
            @Override public void windowGainedFocus(java.awt.event.WindowEvent evt) {}
            @Override public void windowLostFocus(java.awt.event.WindowEvent evt) {
                na.dispose();
            }
        });
    }//GEN-LAST:event_lblNotifAdminMouseClicked

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
            java.util.logging.Logger.getLogger(j_kelolaPengembalianAdmin.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(j_kelolaPengembalianAdmin.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(j_kelolaPengembalianAdmin.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(j_kelolaPengembalianAdmin.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new j_kelolaPengembalianAdmin().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnBack3;
    private javax.swing.JButton btnCariKalender;
    private javax.swing.JButton btnCetakLaporan;
    private javax.swing.JButton btnCetakLaporanHariIni;
    private javax.swing.JButton btnCetakStruk;
    private javax.swing.JButton btnCheckDenda;
    private javax.swing.JButton btnCheckPeminjaman;
    private javax.swing.JButton btnDashboard;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnFilterHariIni;
    private javax.swing.JButton btnKelolaBuku;
    private javax.swing.JButton btnKelolaKategori;
    private javax.swing.JButton btnKelolaPeminjaman;
    private javax.swing.JButton btnKelolaPengembalian;
    private javax.swing.JButton btnKelolaPengunjung;
    private javax.swing.JButton btnKelolaSiswa;
    private javax.swing.JButton btnKonfirmasi;
    private javax.swing.JButton btnPeminjaman;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnSave;
    private javax.swing.JButton btnUpdate;
    private javax.swing.JComboBox<String> cmbBulan;
    private javax.swing.JComboBox<String> cmbTahun;
    private com.toedter.calendar.JDateChooser dateTanggalKembali;
    private com.toedter.calendar.JDateChooser dateTenggatPinjam;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblNotifAdmin;
    private javax.swing.JTable tblReturn;
    private javax.swing.JTextField txtDenda;
    private javax.swing.JTextField txtIdPeminjaman;
    private javax.swing.JTextField txtIdPengembalian;
    private javax.swing.JTextField txtJudul;
    private javax.swing.JTextField txtNama;
    private javax.swing.JTextField txtNisn;
    // End of variables declaration//GEN-END:variables
}
