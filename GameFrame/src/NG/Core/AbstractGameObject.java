package NG.Core;

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
            restoreFields(game);
        }
    }

    /**
     * restores the fields of this game object,
     * @param game
     */
    protected abstract void restoreFields(Game game);
}
