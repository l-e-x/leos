package eu.europa.ec.leos.services.export;

import com.itextpdf.html2pdf.css.CssConstants;
import com.itextpdf.html2pdf.css.apply.impl.BlockCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.BodyTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.CaptionCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.ColTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.ColgroupTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.DisplayTableRowTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.DlTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.HrTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.HtmlTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.LiTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.PageMarginBoxCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.PlaceholderCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.SpanTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.TableTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.TdTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.TrTagCssApplier;
import com.itextpdf.html2pdf.css.apply.impl.UlOlTagCssApplier;
import com.itextpdf.html2pdf.css.resolve.func.counter.PageCountElementNode;
import com.itextpdf.html2pdf.html.TagConstants;
import com.itextpdf.html2pdf.util.TagProcessorMapping;
import com.itextpdf.styledxmlparser.css.page.PageMarginBoxContextNode;
import com.itextpdf.styledxmlparser.css.pseudo.CssPseudoElementUtil;

public class CustomTagCssApplierMapping {

    /** The default mapping. */
    private static TagProcessorMapping mapping;

    static {
        mapping = new TagProcessorMapping();

        mapping.putMapping(TagConstants.A, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.ABBR, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.ADDRESS, BlockCssApplier.class);
        mapping.putMapping(TagConstants.ARTICLE, BlockCssApplier.class);
        mapping.putMapping(TagConstants.ASIDE, BlockCssApplier.class);
        mapping.putMapping(TagConstants.B, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.BDI, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.BDO, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.BLOCKQUOTE, BlockCssApplier.class);
        mapping.putMapping(TagConstants.BODY, BodyTagCssApplier.class);
        mapping.putMapping(TagConstants.BUTTON, BlockCssApplier.class);
        mapping.putMapping(TagConstants.CAPTION, CaptionCssApplier.class);
        mapping.putMapping(TagConstants.CENTER, BlockCssApplier.class);
        mapping.putMapping(TagConstants.CITE, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.CODE, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.COL, ColTagCssApplier.class);
        mapping.putMapping(TagConstants.COLGROUP, ColgroupTagCssApplier.class);
        mapping.putMapping(TagConstants.DD, BlockCssApplier.class);
        mapping.putMapping(TagConstants.DEL, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.DFN, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.DIV, BlockCssApplier.class);
        mapping.putMapping(TagConstants.DL, DlTagCssApplier.class);
        mapping.putMapping(TagConstants.DT, BlockCssApplier.class);
        mapping.putMapping(TagConstants.EM, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.FIELDSET, BlockCssApplier.class);
        mapping.putMapping(TagConstants.FIGCAPTION, BlockCssApplier.class);
        mapping.putMapping(TagConstants.FIGURE, BlockCssApplier.class);
        mapping.putMapping(TagConstants.FONT, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.FOOTER, BlockCssApplier.class);
        mapping.putMapping(TagConstants.FORM, BlockCssApplier.class);
        mapping.putMapping(TagConstants.H1, BlockCssApplier.class);
        mapping.putMapping(TagConstants.H2, BlockCssApplier.class);
        mapping.putMapping(TagConstants.H3, BlockCssApplier.class);
        mapping.putMapping(TagConstants.H4, BlockCssApplier.class);
        mapping.putMapping(TagConstants.H5, BlockCssApplier.class);
        mapping.putMapping(TagConstants.H6, BlockCssApplier.class);
        mapping.putMapping(TagConstants.HEADER, BlockCssApplier.class);
        mapping.putMapping(TagConstants.HR, HrTagCssApplier.class);
        mapping.putMapping(TagConstants.HTML, HtmlTagCssApplier.class);
        mapping.putMapping(TagConstants.I, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.IMG, BlockCssApplier.class);
        mapping.putMapping(TagConstants.INPUT, BlockCssApplier.class);
        mapping.putMapping(TagConstants.INS, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.KBD, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.LABEL, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.LEGEND, BlockCssApplier.class);
        mapping.putMapping(TagConstants.LI, LiTagCssApplier.class);
        mapping.putMapping(TagConstants.MAIN, BlockCssApplier.class);
        mapping.putMapping(TagConstants.MARK, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.NAV, BlockCssApplier.class);
        mapping.putMapping(TagConstants.OBJECT, BlockCssApplier.class);
        mapping.putMapping(TagConstants.OL, UlOlTagCssApplier.class);
        mapping.putMapping(TagConstants.OPTGROUP, BlockCssApplier.class);
        mapping.putMapping(TagConstants.OPTION, BlockCssApplier.class);
        mapping.putMapping(TagConstants.P, BlockCssApplier.class);
        mapping.putMapping(TagConstants.PRE, BlockCssApplier.class);
        mapping.putMapping(TagConstants.Q, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.S, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.SAMP, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.SECTION, BlockCssApplier.class);
        mapping.putMapping(TagConstants.SELECT, BlockCssApplier.class);
        mapping.putMapping(TagConstants.SMALL, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.SPAN, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.STRIKE, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.STRONG, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.SUB, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.SUP, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.SVG, BlockCssApplier.class);
        mapping.putMapping(TagConstants.TABLE, TableTagCssApplier.class);
        mapping.putMapping(TagConstants.TEXTAREA, BlockCssApplier.class);
        mapping.putMapping(TagConstants.TD, TdTagCssApplier.class);
        mapping.putMapping(TagConstants.TFOOT, BlockCssApplier.class);
        mapping.putMapping(TagConstants.TH, TdTagCssApplier.class);
        mapping.putMapping(TagConstants.THEAD, BlockCssApplier.class);
        mapping.putMapping(TagConstants.TIME, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.TR, TrTagCssApplier.class);
        mapping.putMapping(TagConstants.TT, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.U, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.UL, UlOlTagCssApplier.class);
        mapping.putMapping(TagConstants.VAR, SpanTagCssApplier.class);

        String placeholderPseudoElemName = CssPseudoElementUtil.createPseudoElementTagName(CssConstants.PLACEHOLDER);
        mapping.putMapping(placeholderPseudoElemName, PlaceholderCssApplier.class);

        mapping.putMapping(TagConstants.DIV, CssConstants.INLINE, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.UL, CssConstants.INLINE, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.LI, CssConstants.INLINE, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.LI, CssConstants.INLINE_BLOCK, BlockCssApplier.class);
        mapping.putMapping(TagConstants.DD, CssConstants.INLINE, SpanTagCssApplier.class);
        mapping.putMapping(TagConstants.DT, CssConstants.INLINE, SpanTagCssApplier.class);

        mapping.putMapping(TagConstants.SPAN, CssConstants.BLOCK, BlockCssApplier.class);
        mapping.putMapping(TagConstants.SPAN, CssConstants.INLINE_BLOCK, BlockCssApplier.class);
        mapping.putMapping(TagConstants.A, CssConstants.INLINE_BLOCK, BlockCssApplier.class);
        mapping.putMapping(TagConstants.A, CssConstants.BLOCK, BlockCssApplier.class);
        mapping.putMapping(TagConstants.A, CssConstants.TABLE_CELL, BlockCssApplier.class);

        mapping.putMapping(TagConstants.LABEL, CssConstants.BLOCK, BlockCssApplier.class);

        mapping.putMapping(TagConstants.DIV, CssConstants.TABLE, TableTagCssApplier.class);
        mapping.putMapping(TagConstants.DIV, CssConstants.TABLE_CELL, TdTagCssApplier.class);
        mapping.putMapping(TagConstants.DIV, CssConstants.TABLE_ROW, DisplayTableRowTagCssApplier.class);

        // pseudo elements mapping
        String beforePseudoElemName = CssPseudoElementUtil.createPseudoElementTagName(CssConstants.BEFORE);
        String afterPseudoElemName = CssPseudoElementUtil.createPseudoElementTagName(CssConstants.AFTER);
        mapping.putMapping(beforePseudoElemName, SpanTagCssApplier.class);
        mapping.putMapping(afterPseudoElemName, SpanTagCssApplier.class);
        mapping.putMapping(beforePseudoElemName, CssConstants.INLINE_BLOCK, BlockCssApplier.class);
        mapping.putMapping(afterPseudoElemName, CssConstants.INLINE_BLOCK, BlockCssApplier.class);
        mapping.putMapping(beforePseudoElemName, CssConstants.BLOCK, BlockCssApplier.class);
        mapping.putMapping(afterPseudoElemName, CssConstants.BLOCK, BlockCssApplier.class);
        // For now behaving like display:block in display:table case is sufficient
        mapping.putMapping(beforePseudoElemName, CssConstants.TABLE, BlockCssApplier.class);
        mapping.putMapping(afterPseudoElemName, CssConstants.TABLE, BlockCssApplier.class);
        mapping.putMapping(CssPseudoElementUtil.createPseudoElementTagName(TagConstants.IMG), BlockCssApplier.class);

        // custom elements mapping, implementation-specific
        mapping.putMapping(PageCountElementNode.PAGE_COUNTER_TAG, SpanTagCssApplier.class);
        mapping.putMapping(PageMarginBoxContextNode.PAGE_MARGIN_BOX_TAG, PageMarginBoxCssApplier.class);

        // New tags
        mapping.putMapping("akomantoso", BlockCssApplier.class);
        mapping.putMapping("bill", BlockCssApplier.class);
        mapping.putMapping("book", BlockCssApplier.class);
        mapping.putMapping("chapter", BlockCssApplier.class);
        mapping.putMapping("coverpage", BlockCssApplier.class);
        mapping.putMapping("container", BlockCssApplier.class);
        mapping.putMapping("aknp", BlockCssApplier.class);
        mapping.putMapping("longtitle", BlockCssApplier.class);
        mapping.putMapping("docstage", BlockCssApplier.class);
        mapping.putMapping("doctype", BlockCssApplier.class);
        mapping.putMapping("docpurpose", BlockCssApplier.class);
        mapping.putMapping("preamble", BlockCssApplier.class);
        mapping.putMapping("preface", BlockCssApplier.class);
        mapping.putMapping("formula", BlockCssApplier.class);
        mapping.putMapping("citations", BlockCssApplier.class);
        mapping.putMapping("citation", BlockCssApplier.class);
        mapping.putMapping("recitals", BlockCssApplier.class);
        mapping.putMapping("recital", BlockCssApplier.class);
        mapping.putMapping("heading", BlockCssApplier.class);
        mapping.putMapping("num", BlockCssApplier.class);
        mapping.putMapping("tblock", BlockCssApplier.class);
        mapping.putMapping("proviso", BlockCssApplier.class);
        mapping.putMapping("aknbody", BlockCssApplier.class);
        mapping.putMapping("akntitle", BlockCssApplier.class);
        mapping.putMapping("paragraph", BlockCssApplier.class);
        mapping.putMapping("subparagraph", BlockCssApplier.class);
        mapping.putMapping("subsection", BlockCssApplier.class);
        mapping.putMapping("content", BlockCssApplier.class);
        mapping.putMapping("list", BlockCssApplier.class);
        mapping.putMapping("point", BlockCssApplier.class);
        mapping.putMapping("location", BlockCssApplier.class);
        mapping.putMapping("signature", BlockCssApplier.class);
        mapping.putMapping("person", BlockCssApplier.class);
        mapping.putMapping("role", BlockCssApplier.class);
        mapping.putMapping("conclusions", BlockCssApplier.class);
        mapping.putMapping("block", BlockCssApplier.class);

    }

    /**
     * Gets the default CSS applier mapping.
     *
     * @return the default CSS applier mapping
     */
    static TagProcessorMapping getCustomCssApplierMapping() {
        return mapping;
    }

}
