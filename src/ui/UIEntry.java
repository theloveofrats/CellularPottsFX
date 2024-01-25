package ui;

import com.sun.org.apache.xpath.internal.operations.Bool;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.DefaultStringConverter;

import javafx.util.Callback;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.DoubleSummaryStatistics;
import java.util.List;

/**
 * Created by luke on 21/09/17.
 */
public class UIEntry {
    private final StringProperty label = new SimpleStringProperty();
    private final StringProperty value = new SimpleStringProperty();
    private Field targetField;
    private Object target = null;

    public UIEntry(Field field, Object target, String l, String v){
        this.targetField    = field;
        this.target         = target;
        this.label.setValue(l);
        this.value.setValue(v);

        value.addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                System.out.println("Changed in UIEntry class");
                UpdateValue(t1);
            }
        });
    }

    public static Callback<TableColumn<UIEntry, String>, TableCell<UIEntry, String>> GetUIEntryCallback(){
        return new Callback<TableColumn<UIEntry, String>, TableCell<UIEntry, String>>() {
            @Override
            public TextFieldTableCell call(TableColumn<UIEntry, String> col) {
                TextFieldTableCell<UIEntry, String> cell = new TextFieldTableCell<UIEntry, String>() {
                    @Override
                    public void startEdit() {
                        super.startEdit();
                    }

                    @Override
                    public void cancelEdit() {
                        super.cancelEdit();
                        setText(String.valueOf(getItem()));
                    }

                    @Override
                    public void updateItem(String s, boolean empty) {
                        super.updateItem(s, empty);
                        if (empty) return;
                        UIEntry uie = (UIEntry) getTableRow().getItem();
                        if(uie!=null) uie.UpdateValue(s);
                    }
                };
                cell.setConverter(new DefaultStringConverter());
                return cell;
            }
        };
    }

    public static List<UIEntry> GetUILinkedFields(Class c){
        return GetUILinkedFields(c, null);
    }
    public static List<UIEntry> GetUILinkedFields(Class c, Object o){

        List<UIEntry> fields = new ArrayList<>();
        for(Field field  : c.getDeclaredFields()) {
            if (field.isAnnotationPresent(UILink.class)) {

                String label = field.getAnnotation(UILink.class).UILabel();
                String value = "0";
                try{
                    value = field.get(o).toString();
                } catch(IllegalAccessException e){

                }

                if(o==null && !Modifier.isStatic(field.getModifiers())) continue; // Skip fields that don't apply!
                if(o!=null && Modifier.isStatic(field.getModifiers())) continue;
                UIEntry entry = new UIEntry(field, o, label, value);
                fields.add(entry);
            }
        }
        return fields;
    }

    public final StringProperty Label() {
        return this.label;
    }

    public final StringProperty Value() {
        return this.value;
    }

    public void UpdateValue(String s){
        value.setValue(s);
        //System.out.println("Updating value of " + label.getValue());
        Object val = null;
        if(targetField.getType().equals(boolean.class)){
            val = Boolean.parseBoolean(s);
        }
        else if(targetField.getType().equals(double.class)){
            val = Double.parseDouble(s);
        }
        else if(targetField.getType().equals(int.class)){
            val = Integer.parseInt(s);
        }
        try {
            targetField.set(target, val);
        }
        catch(IllegalAccessException e){
            return;
        }
    }
}
