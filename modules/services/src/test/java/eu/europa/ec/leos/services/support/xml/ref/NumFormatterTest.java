/*
 * Copyright 2019 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.services.support.xml.ref;

import eu.europa.ec.leos.test.support.LeosTest;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;

public class NumFormatterTest extends LeosTest {

    @Test
    public void isUnnumbered_unNum_paragraph() {
        //setup
        TreeNode testNode = new TreeNode("paragraph", 0, 1, "id", null, 100, null);
        assertEquals(true, NumFormatter.isUnnumbered(testNode));
    }

    @Test
    public void isUnnumbered_unNum_paragraph2() {
        //setup
        TreeNode testNode = new TreeNode("paragraph", 0, 1, "id", "", 100, null);
        assertEquals(true, NumFormatter.isUnnumbered(testNode));
    }

    @Test
    public void isUnnumbered_unNum_point() {
        //setup
        TreeNode testNode = new TreeNode("point", 0, 1, "id", "-", 100, null);
        assertEquals(true, NumFormatter.isUnnumbered(testNode));
    }

    @Test
    public void isUnnumbered_unNum_point2() {
        //setup
        TreeNode testNode = new TreeNode("point", 0, 1, "id", " ", 100, null);
        assertEquals(true, NumFormatter.isUnnumbered(testNode));
    }

    @Test
    public void isUnnumbered_num_paragraph() {
        //setup
        TreeNode testNode = new TreeNode("paragraph", 0, 1, "id", "1", 100, null);
        assertEquals(false, NumFormatter.isUnnumbered(testNode));
    }

    @Test
    public void formatUnnumbered_first() {
        //setup
        TreeNode testNode = new TreeNode("paragraph", 0, 1, "id", "", 100, null);
        assertEquals("first", NumFormatter.formatUnnumbered(testNode, new Locale("en")));
    }
    
    @Test
    public void formatUnnumbered_first_fr() {
        //setup
        TreeNode testNode = new TreeNode("paragraph", 0, 1, "id", "", 100, null);
        assertEquals("un", NumFormatter.formatUnnumbered(testNode, new Locale("fr")));
    }

    @Test
    public void formatUnnumbered_eighth_en() {
        //setup
        TreeNode testNode = new TreeNode("paragraph", 0, 8, "id", "", 100, null);
        assertEquals("eighth", NumFormatter.formatUnnumbered(testNode, new Locale("en")));
    }

    @Test
    public void formatUnnumbered_eighth_fr() {
        //setup
        TreeNode testNode = new TreeNode("paragraph", 0, 8, "id", "", 100, null);
        //Need to define spellout/ordinal rules for french. as of now using simple representation
        assertEquals("huit", NumFormatter.formatUnnumbered(testNode, new Locale("fr")));
    }

    @Test
    public void formatUnnumbered_third() {
        //setup
        TreeNode testNode = new TreeNode("paragraph", 0, 3, "id", "", 100, null);
        //Need to define spellout/ordinal rules for french. as of now using simple representation
        assertEquals("third", NumFormatter.formatUnnumbered(testNode, new Locale("en")));
    }
    
    @Test
    public void formatPlural_multiple_paragraph() {
        //setup
        TreeNode testNode = new TreeNode("paragraph", 0, 1, "id", "", 100, null);
        assertEquals("paragraphs", NumFormatter.formatPlural(testNode, 0, new Locale("en")));
    }
    
    @Test
    public void formatPlural_single_paragraph() {
        //setup
        TreeNode testNode = new TreeNode("paragraph", 0, 1, "id", "", 100, null);
        assertEquals("paragraph", NumFormatter.formatPlural(testNode, 1, new Locale("en")));
    }
}