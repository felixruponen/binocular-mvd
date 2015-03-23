package cc.arduino.mvd.models;

import com.activeandroid.Model;
import com.activeandroid.annotation.Column;
import com.activeandroid.annotation.Table;
import com.activeandroid.query.Select;

import java.util.List;

/**
 * Created by ksango on 21/03/15.
 */
@Table(name = "bindings")
public class Binding extends Model {

  @Column(name = "service")
  public String service;

  @Column(name = "component")
  public String component;

  @Column(name = "code")
  public String code;

  @Column(name = "pin")
  public String pin;

  public Binding() {
    super();
  }

  public static List<Binding> getAllBindings(String service) {
    return new Select().from(Binding.class).where("service = ?", service).execute();
  }
}
