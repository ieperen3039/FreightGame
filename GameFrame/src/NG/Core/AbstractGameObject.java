package NG.Core;

import NG.Tools.Logger;

import java.lang.reflect.Field;

/**
 * @author Geert van Ieperen created on 25-2-2020.
 */
public abstract class AbstractGameObject implements GameObject {
    protected transient Game game;

    public AbstractGameObject(Game game) {
        this.game = game;
    }

    @Override
    public final void restore(Game game) {
        if (this.game == null) {
            this.game = game;

            // replaces restoreFields(Game game)
            Class<?> thisClass = getClass();
            do {
                // search for GameObject fields.
                for (Field field : thisClass.getDeclaredFields()) {
                    try {
                        if (GameObject.class.isAssignableFrom(field.getType())) {
                            // every GameObject found is restored recursively
                            GameObject fieldInstance = (GameObject) field.get(this);
                            Logger.DEBUG.print("init", fieldInstance);
                            fieldInstance.restore(game);
                        }
                    } catch (ReflectiveOperationException e) {
                        Logger.ERROR.print(e);
                    }
                }
                thisClass = (thisClass.getSuperclass());
            } while (!thisClass.equals(Object.class));
        }
    }
}
