package apigen.adt.api;

abstract public class AbstractType extends aterm.pure.ATermApplImpl {
  protected aterm.ATerm term;

  private apigen.adt.api.Factory abstractTypeFactory;

  public AbstractType(apigen.adt.api.Factory abstractTypeFactory) {
    super(abstractTypeFactory.getPureFactory());
    this.abstractTypeFactory = abstractTypeFactory;
  }

  public void init(int hashCode, aterm.ATermList annos, aterm.AFun fun, aterm.ATerm[] args) {
    super.init(hashCode, annos, fun, args);
  }

  public void initHashCode(aterm.ATermList annos, aterm.AFun fun, aterm.ATerm[] args) {
    super.initHashCode(annos, fun, args);
  }

  abstract public aterm.ATerm toTerm();

  public String toString() {
    return toTerm().toString();
  }

  protected void setTerm(aterm.ATerm term) {
    this.term = term;
  }

  public apigen.adt.api.Factory getApiFactory() {
    return abstractTypeFactory;
  }

  public boolean isSortEntries() {
    return false;
  }

  public boolean isSortEntry() {
    return false;
  }

  public boolean isSortSeparators() {
    return false;
  }

  public boolean isSortSeparator() {
    return false;
  }

  public boolean isSortModules() {
    return false;
  }

  public boolean isSortModule() {
    return false;
  }

  public boolean isSortImports() {
    return false;
  }

  public boolean isSortModuleName() {
    return false;
  }

}
