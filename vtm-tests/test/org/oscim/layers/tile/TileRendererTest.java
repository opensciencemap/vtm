package org.oscim.layers.tile;

import org.junit.Test;
import org.oscim.layers.tile.MapTile.TileNode;
import org.oscim.renderer.MapRenderer;

import static org.fest.assertions.api.Assertions.assertThat;

public class TileRendererTest {

    static {
        /* makes default minFadeTime == 1000 */
        MapRenderer.frametime = 1050;
    }

    static TileNode makeNode(int fadeTime, TileNode parent) {
        TileNode n = new TileNode();
        n.item = new MapTile(n, 0, 0, 0);
        n.item.fadeTime = fadeTime;

        if (parent == null) {
            // root node is its own parent
            n.parent = n;
        } else {
            parent.child00 = n;
            n.parent = parent;
        }

        return n;
    }

    @Test
    public void shouldGetMinFadeTimeOf1000() {

        TileNode r = makeNode(0, null);
        TileNode n = makeNode(0, r);

        long fadeTime = TileRenderer.getMinFade(n.item, 0);

        assertThat(fadeTime).isEqualTo(1000);
    }

    @Test
    public void shouldGetMinFadeTimeFromParent() {

        TileNode r = makeNode(100, null);
        TileNode n = makeNode(0, r);

        long fadeTime = TileRenderer.getMinFade(n.item, 0);

        assertThat(fadeTime).isEqualTo(100);
    }

    @Test
    public void shouldGetMinFadeTimeFromGrandparent() {

        TileNode r = makeNode(100, null);
        TileNode p = makeNode(200, r);
        TileNode n = makeNode(0, p);

        long fadeTime = TileRenderer.getMinFade(n.item, 0);

        assertThat(fadeTime).isEqualTo(100);
    }

    @Test
    public void shouldGetMinFadeTimeFromParentWhenParentsTimeIsLessThanGrandpasTime() {

        TileNode r = makeNode(200, null);
        TileNode p = makeNode(100, r);
        TileNode n = makeNode(0, p);

        long fadeTime = TileRenderer.getMinFade(n.item, 0);

        assertThat(fadeTime).isEqualTo(100);
    }

    @Test
    public void shouldGetMinFadeTimeFromParentForParentProxy() {

        TileNode r = makeNode(200, null);
        TileNode p = makeNode(100, r);
        TileNode n = makeNode(0, p);

        long fadeTime = TileRenderer.getMinFade(n.item, -1);

        assertThat(fadeTime).isEqualTo(100);
    }

    @Test
    public void shouldNotGetMinFadeTimeFromGradparentForParentProxy() {
        /* (as the grandparent will not be locked) */

        TileNode r = makeNode(100, null);
        TileNode p = makeNode(200, r);
        TileNode n = makeNode(0, p);

        long fadeTime = TileRenderer.getMinFade(n.item, -1);

        assertThat(fadeTime).isNotEqualTo(100);
        assertThat(fadeTime).isEqualTo(200);
    }

    @Test
    public void shouldGetMinFadeTimeFromParentForChildProxy() {

        TileNode r = makeNode(200, null);
        TileNode p = makeNode(100, r);
        TileNode n = makeNode(0, p);

        long fadeTime = TileRenderer.getMinFade(n.item, 1);

        assertThat(fadeTime).isEqualTo(100);
    }

    @Test
    public void shouldGetMinFadeTimeFromGrandparentForChildProxy() {

        TileNode r = makeNode(100, null);
        TileNode p = makeNode(200, r);
        TileNode c = makeNode(0, p);

        long fadeTime = TileRenderer.getMinFade(c.item, 1);

        assertThat(fadeTime).isEqualTo(100);
    }

    @Test
    public void shouldGetMinFadeTimeFromGrandgrandparentForChildProxy() {

        TileNode r = makeNode(100, null);
        TileNode gp = makeNode(200, r);
        TileNode p = makeNode(200, gp);
        TileNode c = makeNode(0, p);

        long fadeTime = TileRenderer.getMinFade(c.item, 1);

        assertThat(fadeTime).isEqualTo(100);
    }

    @Test
    public void shouldGetMinFadeTimeFromChild() {

        TileNode r = makeNode(100, null);
        TileNode p = makeNode(200, r);
        TileNode n = makeNode(0, p);

        n.child00 = makeNode(50, n);

        long fadeTime = TileRenderer.getMinFade(n.item, 0);

        assertThat(fadeTime).isEqualTo(50);
    }

    @Test
    public void shouldGetMinFadeTimeForParentFromChild() {

        TileNode r = makeNode(100, null);
        TileNode p = makeNode(200, r);
        TileNode n = makeNode(0, p);

        n.child00 = makeNode(50, n);

        long fadeTime = TileRenderer.getMinFade(p.item, -1);

        assertThat(fadeTime).isEqualTo(50);
    }
}
