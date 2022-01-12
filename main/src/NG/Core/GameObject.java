package NG.Core;

import java.io.Serializable;

/**
 * @author Geert van Ieperen created on 25-2-2020.
 */
public interface GameObject extends Serializable {
    /**
     * restores this game object after being serialized
     * @param game
     */
    void restore(Game game);
}
