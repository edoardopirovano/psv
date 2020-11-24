package psv;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class ActionSet<E> extends LinkedHashSet<E> {
    public ActionSet(Set<E> unsortedAbstract) {
        super(unsortedAbstract);
    }

    public ActionSet() {
        super();
    }

    @Override
    public String toString() {
        Iterator<E> it = iterator();
        if (! it.hasNext())
            return "[]";

        StringBuilder sb = new StringBuilder();
        for (;;) {
            E e = it.next();
            sb.append(e == this ? "(this Collection)" : e);
            if (! it.hasNext())
                return sb.toString();
            sb.append("^");
        }
    }
}
