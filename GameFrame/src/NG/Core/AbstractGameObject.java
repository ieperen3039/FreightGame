package NG.Core;

import NG.Tools.Logger;

import java.lang.reflect.Field;

/**
 * @author Geert van Ieperen created on 25-2-2020.
 */
public abstract class AbstractGameObject implements GameObject {
    private static final boolean REFLECTIVE_INIT = true;
    protected transient Game game;

    public AbstractGameObject(Game game) {
        this.game = game;
    }

//    protected abstract void restoreFields(Game game);

    @Override
    public final void restore(Game game) {
        if (this.game == null) {
            this.game = game;

            // replaces restoreFields(Game game)
            Class<?> theClass = getClass();
            do {
                // search for GameObject fields.
                for (Field field : theClass.getDeclaredFields()) {
                    try {
                        if (GameObject.class.isAssignableFrom(field.getType())) {
                            // every GameObject found is restored recursively
                            GameObject fieldInstance = (GameObject) field.get(this);
                            fieldInstance.restore(game);
                            Logger.DEBUG.print("init", fieldInstance);
                        }
                    } catch (ReflectiveOperationException e) {
                        Logger.ERROR.print(e);
                    }
                }
                theClass = (theClass.getSuperclass());
            } while (!theClass.equals(Object.class));
        }
    }
}
