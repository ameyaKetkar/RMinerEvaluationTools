package refdiff.parsers.java;

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


    public static String getVisibility(int methodModifiers) {
        Visibility visibility;
        if ((methodModifiers & Modifier.PUBLIC) != 0)
            return refdiff.core.cst.Visibility.PUBLIC.name().toLowerCase();
        else if ((methodModifiers & Modifier.PROTECTED) != 0)
            return refdiff.core.cst.Visibility.PROTECTED.name().toLowerCase();
        else if ((methodModifiers & Modifier.PRIVATE) != 0)
            return  refdiff.core.cst.Visibility.PRIVATE.name().toLowerCase();
        else
            return refdiff.core.cst.Visibility.PACKAGE.name().toLowerCase();

    }
    
}
