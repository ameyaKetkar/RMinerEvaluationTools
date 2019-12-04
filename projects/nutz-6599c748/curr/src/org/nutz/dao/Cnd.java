package org.nutz.dao;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.nutz.dao.entity.Entity;
import org.nutz.dao.entity.MappingField;
import org.nutz.dao.jdbc.ValueAdaptor;
import org.nutz.dao.pager.Pager;
import org.nutz.dao.sql.Criteria;
import org.nutz.dao.sql.GroupBy;
import org.nutz.dao.sql.OrderBy;
import org.nutz.dao.sql.Pojo;
import org.nutz.dao.util.cnd.SimpleCondition;
import org.nutz.dao.util.cri.Exps;
import org.nutz.dao.util.cri.SimpleCriteria;
import org.nutz.dao.util.cri.SqlExpression;
import org.nutz.dao.util.cri.SqlExpressionGroup;
import org.nutz.lang.Lang;
import org.nutz.lang.Strings;
import org.nutz.lang.segment.CharSegment;

/**
 * 是 Condition 的一个实现，这个类给你比较方便的方法�?�构建 Condition 接�?�的实例。
 * 
 * <h4>在 Dao 接�?�中使用</h4><br>
 * 
 * 比如一个通常的查询:
 * <p>
 * List<Pet> pets = dao.query(Pet.class,
 * Cnd.where("name","LIKE","B%").asc("name"), null);
 * 
 * <h4>链�?赋值示例</h4><br>
 * Cnd.where("id", ">", 34).and("name","LIKE","T%").asc("name"); <br>
 * 相当于<br>
 * WHERE id>34 AND name LIKE 'T%' ORDER BY name ASC
 * <p>
 * Cnd.orderBy().desc("id"); <br>
 * 相当于<br>
 * ORDER BY id DESC
 * 
 * <h4 style=color:red>你还需�?知�?�的是:</h4><br>
 * <ul>
 * <li>你设置的字段�??，是 java 的字段�?? -- 如果 Entity 里有，那么会被转�?��?数�?�库字段�??
 * <li>如果你设置的是 entity 中�?存在的 java 字段�??，则被认为是数�?�库字段�??，将直接使用
 * <li>你的值，如果是字符串，或者其他类字符串对象（�?�? CharSequence），那么在转�?��? SQL 时，会正确被�?�引�?�包裹
 * <li>你的值如果是�?�?��?�解的自定义对象，会被转化�?字符串处�?�
 * </ul>
 * 
 * @author zozoh(zozohtnt@gmail.com)
 * 
 * @see org.nutz.dao.Condition
 */
public class Cnd implements OrderBy, Criteria, GroupBy {

    /*------------------------------------------------------------------*/
    public static Condition format(String format, Object... args) {
        return Strings.isBlank(format) ? null : new SimpleCondition(format,
                                                                    args);
    }

    public static Condition wrap(String str) {
        return Strings.isBlank(str) ? null : new SimpleCondition((Object) str);
    }

    public static Condition wrap(String sql, Object value) {
        return Strings.isBlank(sql) ? null
                                   : new SimpleCondition(new CharSegment(sql).setBy(value));
    }

    public static SqlExpression exp(String name, String op, Object value) {
        return Exps.create(name, op, value);
    }

    public static SqlExpressionGroup exps(String name, String op, Object value) {
        return exps(exp(name, op, value));
    }

    public static SqlExpressionGroup exps(SqlExpression exp) {
        return new SqlExpressionGroup().and(exp);
    }

    public static Cnd where(String name, String op, Object value) {
        return new Cnd(Cnd.exp(name, op, value));
    }

    public static Cnd where(SqlExpression e) {
        return new Cnd(e);
    }

    public static SimpleCriteria cri() {
        return new SimpleCriteria();
    }

    public static OrderBy orderBy() {
        return new Cnd();
    }

    /**
     * @return 一个 Cnd 的实例
     * @deprecated Since 1.b.50 �?推�??使用这个函数构建 Cnd 的实例，因为看起�?�语�?�?明的样�?
     */
    public static Cnd limit() {
        return new Cnd();
    }

    /**
     * @return 一个 Cnd 的实例
     */
    public static Cnd NEW() {
        return new Cnd();
    }

    public static Cnd byCri(SimpleCriteria cri) {
        return new Cnd().setCri(cri);
    }

    /*------------------------------------------------------------------*/

    private SimpleCriteria cri;

    Cnd() {
        cri = new SimpleCriteria();
    }

    private Cnd setCri(SimpleCriteria cri) {
        this.cri = cri;
        return this;
    }

    public SimpleCriteria getCri() {
        return cri;
    }

    protected Cnd(SqlExpression exp) {
        this();
        cri.where().and(exp);
    }

    public OrderBy asc(String name) {
        cri.asc(name);
        return this;
    }

    public OrderBy desc(String name) {
        cri.desc(name);
        return this;
    }

    public OrderBy orderBy(String name, String dir) {
        if ("asc".equalsIgnoreCase(dir)) {
            this.asc(name);
        } else {
            this.desc(name);
        }
        return this;
    }

    public Cnd and(SqlExpression exp) {
        cri.where().and(exp);
        return this;
    }

    public Cnd and(String name, String op, Object value) {
        return and(Cnd.exp(name, op, value));
    }

    public Cnd or(SqlExpression exp) {
        cri.where().or(exp);
        return this;
    }

    public Cnd or(String name, String op, Object value) {
        return or(Cnd.exp(name, op, value));
    }

    public Cnd andNot(SqlExpression exp) {
        cri.where().and(exp.setNot(true));
        return this;
    }

    public Cnd andNot(String name, String op, Object value) {
        return andNot(Cnd.exp(name, op, value));
    }

    public Cnd orNot(SqlExpression exp) {
        cri.where().or(exp.setNot(true));
        return this;
    }

    public Cnd orNot(String name, String op, Object value) {
        return orNot(Cnd.exp(name, op, value));
    }

    public Pager getPager() {
        return cri.getPager();
    }

    public String toSql(Entity<?> en) {
        return cri.toSql(en);
    }

    public boolean equals(Object obj) {
        return cri.equals(obj);
    }

    public String toString() {
        return cri.toString();
    }

    public void setPojo(Pojo pojo) {
        cri.setPojo(pojo);
    }

    public Pojo getPojo() {
        return cri.getPojo();
    }

    public void joinSql(Entity<?> en, StringBuilder sb) {
        cri.joinSql(en, sb);
    }

    public int joinAdaptor(Entity<?> en, ValueAdaptor[] adaptors, int off) {
        return cri.joinAdaptor(en, adaptors, off);
    }

    public int joinParams(Entity<?> en, Object obj, Object[] params, int off) {
        return cri.joinParams(en, obj, params, off);
    }

    public int paramCount(Entity<?> en) {
        return cri.paramCount(en);
    }

    public SqlExpressionGroup where() {
        return cri.where();
    }

    public GroupBy groupBy(String... names) {
        cri.groupBy(names);
        return this;
    }

    public GroupBy having(Condition cnd) {
        cri.having(cnd);
        return this;
    }

    public OrderBy getOrderBy() {
        return cri.getOrderBy();
    }

    public Cnd limit(int pageNumber, int pageSize) {
        cri.setPager(pageNumber, pageSize);
        return this;
    }

    public Cnd limit(int pageSize) {
        cri.setPager(1, pageSize);
        return this;
    }

    public Cnd limit(Pager pager) {
        cri.setPager(pager);
        return this;
    }
    
    /**
     * 根�?�一个对象生�?Cnd�?�件. 忽略空值/零值,�?忽略Date,�?忽略主键
     * @see org.nutz.dao.Cnd#from(Dao, Object, FieldFilter)
     * @param dao Dao实例
     * @param obj 基对象,�?�?�以是Class,字符串,数值和Boolean
     */
    public static Cnd from(Dao dao, Object obj) {
        return from(dao, obj, null);
    }

    /**
     * 根�?�一个对象生�?Cnd�?�件. 按filter过滤属性, 且忽略空值/零值,�?忽略Date,�?忽略主键
     * @param dao Dao实例
     * @param obj 基对象,�?�?�以是Class,字符串,数值和Boolean
     * @param filter 过滤字段属性
     * @return Cnd�?�件
     */
    public static Cnd from(Dao dao, Object obj, FieldFilter filter) {
        return from(dao, obj, filter, true, true, false, false, false, false);
    }
    
    /**
     * 根�?�一个对象生�?Cnd�?�件. 按filter过滤属性,�?忽略主键
     * @param dao Dao实例
     * @param obj 基对象,�?�?�以是Class,字符串,数值和Boolean
     * @param filter 过滤字段属性
     * @param ignoreNull 是�?�忽略空值
     * @param ignoreZero 是�?�忽略0值
     * @param ignoreDate 是�?�忽略java.util.Date类�?�其�?类的对象
     * @return Cnd�?�件
     */
    public static Cnd from(Dao dao, Object obj, FieldFilter filter,
                           boolean ignoreNull, boolean ignoreZero, boolean ignoreDate) {
        return from(dao, obj, filter, ignoreNull, ignoreZero, ignoreDate, false, false, false);
    }
    
    /**
     * 根�?�一个对象生�?Cnd�?�件
     * @param dao Dao实例
     * @param obj 基对象,�?�?�以是Class,字符串,数值和Boolean
     * @param filter 属性过滤
     * @param ignoreNull 是�?�忽略空值
     * @param ignoreZero 是�?�忽略0值
     * @param ignoreDate 是�?�忽略java.util.Date类�?�其�?类的对象
     * @param ignoreId   是�?�忽略@Id所标注的主键属性
     * @param ignoreName 是�?�忽略 \@Name 所标注的主键属性
     * @param ignorePk   是�?�忽略 \@Pk 所引用的�?�?�主键 
     * @return Cnd�?�件
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Cnd from(Dao dao, Object obj, FieldFilter filter, 
                           boolean ignoreNull, boolean ignoreZero, boolean ignoreDate, 
                           boolean ignoreId,
                           boolean ignoreName,
                           boolean ignorePk
                           ) {
        if (obj == null)
            return null;
        obj = Lang.first(obj);
        if (obj == null) {
            return null;
        }
        if (obj.getClass() == Class.class) {
            throw Lang.impossible();
        }
        if (obj instanceof String || obj instanceof Number || obj instanceof Boolean) {
            throw Lang.impossible();
        }
        Entity en = dao.getEntity(obj.getClass());
        if (en == null) {
            throw Lang.impossible();
        }
        
        List<MappingField> mfs = en.getMappingFields();
        if (filter != null) {
            FieldMatcher fm = filter.map().get(obj.getClass());
            if (fm != null) {
                Iterator<MappingField> it = mfs.iterator();
                while (it.hasNext()) {
                    MappingField mf = it.next();
                    if (!fm.match(mf.getName()))
                        it.remove();
                }
            }
        }
        
        Cnd cnd = Cnd.NEW();
        for (MappingField mf : mfs) {
            if (ignoreId && mf.isId())
                continue;
            if (ignoreName && mf.isName())
                continue;
            if (ignorePk && mf.isCompositePk())
                continue;
            Object val = mf.getValue(obj);
            if (val == null) {
                if (ignoreNull)
                    continue;
            } if (val instanceof Number && ((Number)val).doubleValue() == 0.0) {
                if (ignoreZero)
                    continue;
            } if (val instanceof Date) {
                if (ignoreDate)
                    continue;
            }
            cnd.and(mf.getName(), "=", val);
        }
        return cnd;
    }
}
