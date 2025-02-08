package demo.event;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EventCSVC {
	
	private JTable tb1;
	private int maTuDong = 0;
    private static final String chuoiKN = "jdbc:mysql://localhost:3306/QLTV?useSSL=false&allowPublicKeyRetrieval=true";

    public EventCSVC(JTextField tf1, JTextField tf2, JTextField tf3,JTable tb1) {
    	this.tb1 = tb1;
    }
    public void handleAdd(JTextField txtTen, JTextField txtTinhTrang, JSpinner spnNgayMua, JSpinner spnNgayBT, JFrame frame) {
        String tenThietBi = txtTen.getText().trim();
        String tinhTrangThietBi = txtTinhTrang.getText().trim();
        String ngayMua = spnNgayMua.getValue().toString();
        String ngayBaoTri = spnNgayBT.getValue().toString();
        
        if (tenThietBi.isEmpty() || tinhTrangThietBi.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Vui lòng điền đầy đủ thông tin", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DriverManager.getConnection(chuoiKN, "root", "123456")) {
            String countQuery = "SELECT COUNT(*) FROM coSoVatChat";
            try (PreparedStatement stmtCount = conn.prepareStatement(countQuery)) {
                ResultSet rs = stmtCount.executeQuery();
                if (rs.next()) {
                    maTuDong = rs.getInt(1) + 1;
                }
            }

            String deviceID = String.format("D%06d", maTuDong);

            String checkQuery = "SELECT COUNT(*) FROM coSoVatChat WHERE maThietBi = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {
                checkStmt.setString(1, deviceID);
                ResultSet checkRS = checkStmt.executeQuery();
                if (checkRS.next() && checkRS.getInt(1) > 0) {
                    JOptionPane.showMessageDialog(frame, "Device ID already exists!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            String insertQuery = "INSERT INTO coSoVatChat (maThietBi, tenThietBi, tinhTrangThietBi, ngayMua, ngayBaoTri) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setString(1, deviceID);
                insertStmt.setString(2, tenThietBi);
                insertStmt.setString(3, tinhTrangThietBi);
                insertStmt.setString(4, ngayMua);
                insertStmt.setString(5, ngayBaoTri);
                int rowsAffected = insertStmt.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(frame, "Device added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);

                    // Insert into Kho (inventory)
                    String insertKhoQuery = "INSERT INTO Kho (maThietBi, soLuong) VALUES (?, ?)";
                    try (PreparedStatement insertKhoStmt = conn.prepareStatement(insertKhoQuery)) {
                        insertKhoStmt.setString(1, deviceID);
                        insertKhoStmt.setInt(2, 1); // Assuming quantity is 1 for a new device
                        insertKhoStmt.executeUpdate();
                    }
                    maTuDong++;
                } else {
                    JOptionPane.showMessageDialog(frame, "Failed to add device!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(frame, "Database connection error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void loadIdCSVC(JTextField textField) {
        try (Connection conn = DriverManager.getConnection(chuoiKN, "root", "123456")) {
            try {
                String sql = "SELECT idThietBi FROM coSoVatChat ORDER BY idThietBi DESC LIMIT 1";
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                if (rs.next()) {
                    String lastDeviceCode = rs.getString("idThietBi");
                    int lastNumber = Integer.parseInt(lastDeviceCode.substring(2));

                    int nextNumber = lastNumber + 1;
                    String nextDeviceCode = "TB" + String.format("%03d", nextNumber);

                    textField.setText(nextDeviceCode);
                } else {
                    textField.setText("TB001");
                }

                rs.close();
                stmt.close();
                conn.close();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
        }
    }

    public void handleEdit(JTextField idThietBi, JTextField tenThietBi, JTextField tinhTrangThietBi, JSpinner spnNgayMua, JSpinner spnNgayBT) {
        String idThietBiText = idThietBi.getText().trim();
        if (idThietBiText.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Vui lòng chọn thiết bị để sửa", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String tenThietBiText = tenThietBi.getText().trim();
        String tinhTrangThietBiText = tinhTrangThietBi.getText().trim();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String ngayMua = sdf.format(spnNgayMua.getValue());
        String ngayBaoTri = sdf.format(spnNgayBT.getValue());

        try (Connection conn = DriverManager.getConnection(chuoiKN, "root", "123456")) {
            String sql = "UPDATE coSoVatChat SET tenThietBi = ?, tinhTrangThietBi = ?, ngayMua = ?, ngayBaoTri = ? WHERE idThietBi = ?";
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, tenThietBiText);
            pst.setString(2, tinhTrangThietBiText);
            pst.setString(3, ngayMua);
            pst.setString(4, ngayBaoTri);
            pst.setString(5, idThietBiText);

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                JOptionPane.showMessageDialog(null, "Cập nhật thông tin thiết bị thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);

                DefaultTableModel model = new DefaultTableModel();
                int selectedRow = tb1.getSelectedRow();
                if (selectedRow >= 0) {
                    model.setValueAt(tenThietBiText, selectedRow, 1);
                    model.setValueAt(tinhTrangThietBiText, selectedRow, 2);
                    model.setValueAt(ngayMua, selectedRow, 3);
                    model.setValueAt(ngayBaoTri, selectedRow, 4);
                }

                btnReset_Click(idThietBi, tenThietBi, tinhTrangThietBi, spnNgayMua, spnNgayBT);
            } else {
                JOptionPane.showMessageDialog(null, "Không tìm thấy thiết bị để sửa", "Thông báo", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Lỗi kết nối cơ sở dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    public void loadData() {
        try (Connection conn = DriverManager.getConnection(chuoiKN, "root", "123456");
             Statement stmt = conn.createStatement()) {
            String sql = "SELECT * FROM coSoVatChat";
            ResultSet rs = stmt.executeQuery(sql);

            DefaultTableModel model = new DefaultTableModel();
            model.setRowCount(0);
            model.addColumn("Mã thiết bị");
            model.addColumn("Tên thiết bị");
            model.addColumn("Tình trạng");
            model.addColumn("Ngày mua");
            model.addColumn("Ngày bảo trì");
            
            while (rs.next()) {
                String idThietBi = rs.getString("idThietBi");
                String tenThietBi = rs.getString("tenThietBi");
                String tinhTrangThietBi = rs.getString("tinhTrangThietBi");
                String ngayMua = rs.getString("ngayMua");
                String ngayBaoTri = rs.getString("ngayBaoTri");

                model.addRow(new Object[]{idThietBi, tenThietBi, tinhTrangThietBi, ngayMua, ngayBaoTri});
            }
            tb1.setModel(model);
            tb1.revalidate();
            tb1.repaint();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Lỗi kết nối cơ sở dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    public void EventCSVC_Load() {
    	loadData();
    }

    public void handleDelete(JTextField idThietBi, JTextField tenThietBi, JTextField tinhTrangThietBi, JSpinner spnNgayMua, JSpinner spnNgayBT, JFrame currentFrame) {
        String idThietBiText = idThietBi.getText().trim();
        if (idThietBiText.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Vui lòng chọn thiết bị để xóa", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(null, "Bạn có chắc chắn muốn xóa thiết bị này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection(chuoiKN, "root", "123456")) {
                String sql = "DELETE FROM coSoVatChat WHERE idThietBi = ?";
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, idThietBiText);

                int rowsAffected = pst.executeUpdate();

                if (rowsAffected > 0) {
                    JOptionPane.showMessageDialog(null, "Xóa thiết bị thành công!", "Thông báo", JOptionPane.INFORMATION_MESSAGE);

                    DefaultTableModel model = new DefaultTableModel();
                    int selectedRow = tb1.getSelectedRow();
                    if (selectedRow >= 0) {
                        model.removeRow(selectedRow);
                    }

                    btnReset_Click(idThietBi, tenThietBi, tinhTrangThietBi, spnNgayMua, spnNgayBT);
                } else {
                    JOptionPane.showMessageDialog(null, "Không tìm thấy thiết bị để xóa", "Thông báo", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(null, "Lỗi kết nối cơ sở dữ liệu: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    public void btnReset_Click(JTextField idThietBi, JTextField tenThietBi, JTextField tinhTrangThietBi, JSpinner spnNgayMua, JSpinner spnNgayBT) {
    	idThietBi.setText("");
        tenThietBi.setText("");
        tinhTrangThietBi.setText("");
        spnNgayMua.setValue(new Date());
        spnNgayBT.setValue(new Date());
    }

    public void btnThoat_Click(JFrame currentFrame) {
        currentFrame.dispose();
        demo.main.MainChinh.main(null);
    }
}
