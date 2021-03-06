/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.kles.view.mi;

import com.kles.MainApp;
import com.kles.mi.FieldMetadata;
import com.kles.mi.MIRecord;
import com.kles.mi.MIResultInput;
import com.kles.output.AbstractOutput;
import com.kles.output.CSVTableView;
import com.kles.output.xls.ExcelTableView;
import com.kles.utils.MIUtils;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.controlsfx.control.PopOver;

/**
 * FXML Controller class
 *
 * @author jchau
 */
public class MIResultTableViewController {

    @FXML
    public VBox root;
    @FXML
    public TableView table;
    @FXML
    public Button bExcel, bCSV;

    private BooleanProperty isCloseDisable, isSaveDisable, isTableDisable;

    private MIResultInput midata;

    private PopOver popOver;
    private Node popNodeParent;
    private MainApp mainApp;

    @FXML
    public void initialize() {
        isCloseDisable = new SimpleBooleanProperty(false);
        isSaveDisable = new SimpleBooleanProperty(false);
        isTableDisable = new SimpleBooleanProperty(false);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.disableProperty().bind(isTableDisable);
        bExcel.disableProperty().bind(isCloseDisable);
        bCSV.disableProperty().bind(isSaveDisable);
        table.setOnMousePressed((MouseEvent event) -> {
            if (event.isPrimaryButtonDown() && event.getClickCount() == 2) {
                popNodeParent = table;
                handleEdit();
            }
        });
    }

    public void buildTable() {
        if (midata != null) {
            if (midata.getResult() != null) {
                table.getColumns().clear();
                if (midata.getResult().getMetadata() != null) {
                    for (int i = 0; i < midata.getResult().getMetadata().getField().size(); i++) {
                        final int finalIdx = i;
                        final FieldMetadata f = midata.getResult().getMetadata().getField().get(i);
                        final TableColumn<MIRecord, String> col = new TableColumn<>();
                        col.setText(f.getDescription() + "\n" + f.getName());
                        col.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getNameValue().get(finalIdx).getValue()));
                        table.getColumns().add(col);
                    }
                    if (midata.getResult().getMIRecord() != null) {
                        table.setItems(FXCollections.observableList(midata.getResult().getMIRecord()));
                    }
                } else {
                    table.setPlaceholder(new Label("Result:OK"));
                }
            }
        }
    }

    private void handleEdit() {
        Platform.runLater(() -> {
            try {
                if (popOver != null && popOver.isShowing()) {
                    popOver.hide(Duration.ZERO);
                }
                com.sun.glass.ui.Robot robot = com.sun.glass.ui.Application.GetApplication().createRobot();
                int x = robot.getMouseX();
                int y = robot.getMouseY();
                popOver = createPopOver();
                popOver.show(popNodeParent, x, y);
            } catch (IOException ex) {
                Logger.getLogger(MIResultTableViewController.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    private PopOver createPopOver() throws IOException {
        popOver = new PopOver();
        popOver.setDetachable(true);
        popOver.setDetached(false);
        popOver.setArrowSize(20);
        if (table.getSelectionModel().getSelectedItem() != null) {
//            popOver.setContentNode(buildOutputPanel());
            popOver.setContentNode(MIUtils.buildOutputPanel((MIRecord) table.getSelectionModel().getSelectedItem(), midata.getResult()));
            return popOver;
        }
        return null;
    }

    private Node buildOutputPanel() {
        ScrollPane scroll = new ScrollPane();
        scroll.setPrefSize(350, 595);
        GridPane g = new GridPane();
        g.setHgap(5d);
        g.setVgap(5d);
        g.setPadding(new Insets(5d));
        MIRecord oData = (MIRecord) table.getSelectionModel().getSelectedItem();
        int row = 0;
        if (oData != null) {
            for (FieldMetadata f : midata.getResult().getMetadata().getField()) {
                final Label l = new Label(f.getName());
                l.setFont(Font.font(null, FontWeight.BOLD, 15));
                l.setTooltip(new Tooltip(f.getDescription()));
                final TextField t = new TextField();
                t.setEditable(false);
                if (oData.getNameValue().get(row) != null) {
                    t.setText(oData.getNameValue().get(row).getValue());
                }
                t.setTooltip(new Tooltip(f.getDescription()));
                g.add(l, 0, row);
                g.add(t, 1, row);
                row++;
            }
        }
        scroll.setContent(g);
        return scroll;
    }

    @FXML
    public void csv() {
        FileChooser f = new FileChooser();
        f.setTitle("CSV");
        f.setInitialFileName(midata.getResult().getTransaction() + ".csv");
        f.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("CSV", new String[]{".csv"}));
        File file = f.showSaveDialog(this.getRoot().getScene().getWindow());
        if (file != null) {
            CSVTableView csv = new CSVTableView();
            csv.setFile(file);
            csv.setTableView(table);
            String[] tabHeader = new String[midata.getResult().getMetadata().getField().size()];
            for (int i = 0; i < midata.getResult().getMetadata().getField().size(); i++) {
                tabHeader[i] = midata.getResult().getMetadata().getField().get(i).getName();
            }
            csv.setHeader(tabHeader);
            csv.setData(MIUtils.tableDataMIToListString(table));
            csv.setAction(AbstractOutput.WRITE);
            csv.run();
        }
    }

    @FXML
    public void excel() {
        FileChooser f = new FileChooser();
        f.setTitle("Excel");
        f.setInitialFileName(midata.getResult().getTransaction() + ".xls");
        f.setSelectedExtensionFilter(new FileChooser.ExtensionFilter("Excel", new String[]{".xls"}));
        File file = f.showSaveDialog(this.getRoot().getScene().getWindow());
        if (file != null) {
            ExcelTableView xls = new ExcelTableView(table);
            xls.setFilePath(file.getAbsolutePath());
            xls.setAction(AbstractOutput.WRITE);
            xls.run();
        }
    }

    @FXML
    public void save() {

    }

    public void setMainApp(MainApp main) {
        mainApp = main;
    }

    public VBox getRoot() {
        return root;
    }

    public void setRoot(VBox root) {
        this.root = root;
    }

    public TableView getTable() {
        return table;
    }

    public void setTable(TableView table) {
        this.table = table;
    }

    public MIResultInput getMIData() {
        return midata;
    }

    public void setMIData(MIResultInput midata) {
        this.midata = midata;
    }

    public Button getCloseButton() {
        return bExcel;
    }

    public Button getSaveButton() {
        return bCSV;
    }

    public BooleanProperty getIsCloseDisable() {
        return isCloseDisable;
    }

    public BooleanProperty getIsSaveDisable() {
        return isSaveDisable;
    }

    public BooleanProperty getIsTableDisable() {
        return isTableDisable;
    }
}
