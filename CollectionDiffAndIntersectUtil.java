import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class CollectionDiffAndIntersectUtil {

  /**
   * 從一組list轉換為指定的getter method取得得直為key，value為list的一對多的map
   * 
   * @param methodNameOfKey
   * @param keyType
   * @param entities
   * @return
   */
  public static final <K, T> Map<K, List<T>> getGroupKeyMapping(String methodNameOfKey, Class<K> keyType, Collection<T> entities) {
    return getGroupKeyMapping(methodNameOfKey, keyType, entities, true, false);
  }

  /**
   * 從一組list轉換為指定的getter method取得得直為key，value為list的一對多的map。可指定是否忽略null值及資料是否被攤平
   * (攤平：key中有list會被攤成一個一個的item)
   * 
   * @param methodNameOfKey
   * @param keyType
   * @param entities
   * @return
   */
  public static final <K, T> Map<K, List<T>> getGroupKeyMapping(String methodNameOfKey, Class<K> keyType, Collection<T> entities, boolean ignoreNullKey,
      boolean flattenList) {

    Map<K, List<T>> map = new LinkedHashMap<K, List<T>>();

    Class noparams[] = {};
    Object[] emptyparam = new Object[] {};
    if (entities != null) {
      for (T entity : entities) {
        Class cls;
        try {
          Object tempK;
          if (entity instanceof Map) {
            tempK = ((Map) entity).get(methodNameOfKey);
          } else {
            cls = Class.forName(entity.getClass().getName());
            // Method method = cls.getDeclaredMethod(methodNameOfKey, noparams);
            Method method = cls.getMethod(methodNameOfKey, noparams);
            tempK = method.invoke(entity, emptyparam);
          }
          List<K> lk = new ArrayList<K>();
          if (flattenList && tempK instanceof List) {
            for (Object ok : (List) tempK) {
              lk.add((K) ok);
            }
          } else {
            lk.add((K) tempK);
          }
          for (K k : lk) {
            if (ignoreNullKey && k == null) {
              continue;
            }
            List<T> v = map.get(k);
            if (v == null) {
              v = new ArrayList<T>();
              map.put(k, v);
            }
            v.add(entity);
          }
        } catch (Exception e) {
          logger.debug("Some thing wrong when change entity to map", e);
        }
      }
    }
    return map;
  }

  /**
   * 比較兩個collection,回傳兩個collection的差集以及交集 此方法使用Hash,速度上比iterator的方法快
   * 但只能以entity的某一個param做為key
   * 
   * @param collectionA
   *          新的collection
   * @param collectionB
   *          舊的collection
   * @param methodNameOfKey
   *          key的getter
   * @param keyType
   *          key的ClassType
   * @param returnA
   *          intersect是否回傳collectionA中的entity, 反之則回傳B
   * @return DiffAndIntersect Class 分別可以getDiffA, getDiffB,
   *         getIntersect取得List<K>
   */
  public static <K, T> DiffAndIntersect<K> hashDiffAndIntersect(List<K> collectionA, List<K> collectionB, String methodNameOfKey, Class<T> keyType,
      boolean returnA) {

    List<K> diffA = new ArrayList<K>();
    List<K> diffB = new ArrayList<K>();
    List<K> intersect = new ArrayList<K>();

    Map<T, List<K>> setA = getGroupKeyMapping(methodNameOfKey, keyType, collectionA);
    Map<T, List<K>> setB = getGroupKeyMapping(methodNameOfKey, keyType, collectionB);

    Set<T> allSet = new HashSet<T>();
    allSet.addAll(setA.keySet());
    allSet.addAll(setB.keySet());
    for (T t : allSet) {
      if (setA.get(t) == null || setA.get(t).isEmpty()) {
        diffB.addAll(setB.get(t));
      } else if (setB.get(t) == null || setB.get(t).isEmpty()) {
        diffA.addAll(setA.get(t));
      } else {
        if (returnA)
          intersect.addAll(setA.get(t));
        else
          intersect.addAll(setB.get(t));
      }
    }

    DiffAndIntersect<K> result = new DiffAndIntersect<K>(diffA, diffB, intersect);

    return result;

  }

  /**
   * 比較兩個collection,回傳兩個collection的差集以及交集 此方法需自行實作EntityComparator 效能較Hash方法慢
   * 
   * @param collectionA
   *          新的collection
   * @param collectionB
   *          舊的collection
   * @param comparator
   *          請自行實作EntityComparator的compare方法
   * @param returnNew
   *          intersect是否回傳collectionA中的entity, 反之則回傳B
   * @return DiffAndIntersect Class 分別可以getDiffA, getDiffB,
   *         getIntersect取得List<K>
   */
  public static <K> DiffAndIntersect<K> iterateDiffAndIntersect(List<K> collectionA, List<K> collectionB, EntityComparator<K> comparator, boolean returnNew) {

    List<K> diffA = new ArrayList<K>();
    List<K> diffB = new ArrayList<K>();
    List<K> intersect = new ArrayList<K>();

    for (K entity : collectionB) {

      if (containsCheck(collectionA, entity, comparator)) {
        intersect.add(entity);
        removeFrom(collectionA, entity, comparator);
      } else {
        diffB.add(entity);
      }
    }
    diffA.addAll(collectionA);

    DiffAndIntersect<K> result = new DiffAndIntersect<K>(diffA, diffB, intersect);

    return result;

  }

  public static class DiffAndIntersect<K> {

    private List<K> diffA;
    private List<K> diffB;
    private List<K> intersect;

    public DiffAndIntersect(List<K> diffA, List<K> diffB, List<K> intersect) {
      super();
      this.diffA = diffA;
      this.diffB = diffB;
      this.intersect = intersect;
    }

    public List<K> getDiffA() {
      return diffA;
    }

    public void setDiffA(List<K> diffA) {
      this.diffA = diffA;
    }

    public List<K> getDiffB() {
      return diffB;
    }

    public void setDiffB(List<K> diffB) {
      this.diffB = diffB;
    }

    public List<K> getIntersect() {
      return intersect;
    }

    public void setIntersect(List<K> intersect) {
      this.intersect = intersect;
    }

  }

  public static <K> boolean containsCheck(List<K> collection, K entity, EntityComparator<K> comparator) {

    for (K k : collection)
      if (comparator.compare(k, entity))
        return true;
    return false;
  }

  public static <K> void removeFrom(List<K> collection, K entity, EntityComparator<K> comparator) {

    for (K k : collection)
      if (comparator.compare(k, entity))
        collection.remove(k);
  }

}
