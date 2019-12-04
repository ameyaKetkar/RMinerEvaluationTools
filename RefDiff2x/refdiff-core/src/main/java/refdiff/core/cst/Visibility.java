package refdiff.core.cst;

import org.eclipse.jdt.core.dom.Modifier;

public enum Visibility {

    PUBLIC,
    PRIVATE,
    PROTECTED,
    PACKAGE;
    
    @Override
    public String toString() {
        return this.name().toLowerCase();
    }


    @SuppressWarnings("unused")
    public static Visibility getVisibility(int methodModifiers) {
        Visibility visibility;
        if ((methodModifiers & Modifier.PUBLIC) != 0)
            visibility = Visibility.PUBLIC;
        else if ((methodModifiers & Modifier.PROTECTED) != 0)
            visibility = Visibility.PROTECTED;
        else if ((methodModifiers & Modifier.PRIVATE) != 0)
            visibility = Visibility.PRIVATE;
        else
            visibility = Visibility.PACKAGE;
        return visibility;
    }
}
