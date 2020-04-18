package eu.europa.ec.leos.services.export;

import com.itextpdf.html2pdf.attach.impl.tags.ABlockTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.ATagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.AbbrTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.BodyTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.BrTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.ButtonTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.CaptionTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.ColTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.ColgroupTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.DisplayTableRowTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.DisplayTableTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.DivTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.HTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.HrTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.HtmlTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.ImgTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.InputTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.LiTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.LinkTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.MetaTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.ObjectTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.OptGroupTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.OptionTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.PTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.PageCountWorker;
import com.itextpdf.html2pdf.attach.impl.tags.PageMarginBoxWorker;
import com.itextpdf.html2pdf.attach.impl.tags.PlaceholderTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.PreTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.SelectTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.SpanTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.SvgTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.TableFooterTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.TableHeaderTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.TableTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.TdTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.TextAreaTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.ThTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.TitleTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.TrTagWorker;
import com.itextpdf.html2pdf.attach.impl.tags.UlOlTagWorker;
import com.itextpdf.html2pdf.css.CssConstants;
import com.itextpdf.html2pdf.css.resolve.func.counter.PageCountElementNode;
import com.itextpdf.html2pdf.html.TagConstants;
import com.itextpdf.html2pdf.util.TagProcessorMapping;
import com.itextpdf.styledxmlparser.css.page.PageMarginBoxContextNode;
import com.itextpdf.styledxmlparser.css.pseudo.CssPseudoElementUtil;

public class CustomTagWorkerMapping {

    /**
     * The worker mapping.
     */
    private static TagProcessorMapping workerMapping;

    static {
        workerMapping = new TagProcessorMapping();
        workerMapping.putMapping(TagConstants.A, ATagWorker.class);
        workerMapping.putMapping(TagConstants.ABBR, AbbrTagWorker.class);
        workerMapping.putMapping(TagConstants.ADDRESS, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.ARTICLE, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.ASIDE, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.B, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.BDI, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.BDO, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.BLOCKQUOTE, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.BODY, BodyTagWorker.class);
        workerMapping.putMapping(TagConstants.BR, BrTagWorker.class);
        workerMapping.putMapping(TagConstants.BUTTON, ButtonTagWorker.class);
        workerMapping.putMapping(TagConstants.CAPTION, CaptionTagWorker.class);
        workerMapping.putMapping(TagConstants.CENTER, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.CITE, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.CODE, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.COL, ColTagWorker.class);
        workerMapping.putMapping(TagConstants.COLGROUP, ColgroupTagWorker.class);
        workerMapping.putMapping(TagConstants.DD, LiTagWorker.class);
        workerMapping.putMapping(TagConstants.DEL, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.DFN, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.DIV, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.DL, UlOlTagWorker.class);
        workerMapping.putMapping(TagConstants.DT, LiTagWorker.class);
        workerMapping.putMapping(TagConstants.EM, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.FIELDSET, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.FIGCAPTION, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.FIGURE, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.FONT, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.FOOTER, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.FORM, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.H1, HTagWorker.class);
        workerMapping.putMapping(TagConstants.H2, HTagWorker.class);
        workerMapping.putMapping(TagConstants.H3, HTagWorker.class);
        workerMapping.putMapping(TagConstants.H4, HTagWorker.class);
        workerMapping.putMapping(TagConstants.H5, HTagWorker.class);
        workerMapping.putMapping(TagConstants.H6, HTagWorker.class);
        workerMapping.putMapping(TagConstants.HEADER, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.HR, HrTagWorker.class);
        workerMapping.putMapping(TagConstants.HTML, HtmlTagWorker.class);
        workerMapping.putMapping(TagConstants.I, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.IMG, ImgTagWorker.class);
        workerMapping.putMapping(TagConstants.INPUT, InputTagWorker.class);
        workerMapping.putMapping(TagConstants.INS, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.KBD, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.LABEL, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.LEGEND, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.LI, LiTagWorker.class);
        workerMapping.putMapping(TagConstants.LINK, LinkTagWorker.class);
        workerMapping.putMapping(TagConstants.MAIN, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.MARK, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.META, MetaTagWorker.class);
        workerMapping.putMapping(TagConstants.NAV, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.OBJECT, ObjectTagWorker.class);
        workerMapping.putMapping(TagConstants.OL, UlOlTagWorker.class);
        workerMapping.putMapping(TagConstants.OPTGROUP, OptGroupTagWorker.class);
        workerMapping.putMapping(TagConstants.OPTION, OptionTagWorker.class);
        workerMapping.putMapping(TagConstants.P, PTagWorker.class);
        workerMapping.putMapping(TagConstants.PRE, PreTagWorker.class);
        workerMapping.putMapping(TagConstants.Q, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.S, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.SAMP, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.SECTION, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.SELECT, SelectTagWorker.class);
        workerMapping.putMapping(TagConstants.SMALL, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.SPAN, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.STRIKE, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.STRONG, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.SUB, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.SUP, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.SVG, SvgTagWorker.class);
        workerMapping.putMapping(TagConstants.TABLE, TableTagWorker.class);
        workerMapping.putMapping(TagConstants.TD, TdTagWorker.class);
        workerMapping.putMapping(TagConstants.TEXTAREA, TextAreaTagWorker.class);
        workerMapping.putMapping(TagConstants.TFOOT, TableFooterTagWorker.class);
        workerMapping.putMapping(TagConstants.TH, ThTagWorker.class);
        workerMapping.putMapping(TagConstants.THEAD, TableHeaderTagWorker.class);
        workerMapping.putMapping(TagConstants.TIME, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.TITLE, TitleTagWorker.class);
        workerMapping.putMapping(TagConstants.TR, TrTagWorker.class);
        workerMapping.putMapping(TagConstants.TT, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.U, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.UL, UlOlTagWorker.class);
        workerMapping.putMapping(TagConstants.VAR, SpanTagWorker.class);

        String placeholderPseudoElemName = CssPseudoElementUtil.createPseudoElementTagName(CssConstants.PLACEHOLDER);
        workerMapping.putMapping(placeholderPseudoElemName, PlaceholderTagWorker.class);

        workerMapping.putMapping(TagConstants.UL, CssConstants.INLINE, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.LI, CssConstants.INLINE, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.LI, CssConstants.INLINE_BLOCK, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.DD, CssConstants.INLINE, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.DT, CssConstants.INLINE, SpanTagWorker.class);

        workerMapping.putMapping(TagConstants.SPAN, CssConstants.BLOCK, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.SPAN, CssConstants.INLINE_BLOCK, DivTagWorker.class);
        workerMapping.putMapping(TagConstants.A, CssConstants.BLOCK, ABlockTagWorker.class);
        workerMapping.putMapping(TagConstants.A, CssConstants.INLINE_BLOCK, ABlockTagWorker.class);
        workerMapping.putMapping(TagConstants.A, CssConstants.TABLE_CELL, ABlockTagWorker.class);

        workerMapping.putMapping(TagConstants.LABEL, CssConstants.BLOCK, DivTagWorker.class);

        workerMapping.putMapping(TagConstants.DIV, CssConstants.TABLE, DisplayTableTagWorker.class);
        workerMapping.putMapping(TagConstants.DIV, CssConstants.TABLE_ROW, DisplayTableRowTagWorker.class);
        workerMapping.putMapping(TagConstants.DIV, CssConstants.INLINE, SpanTagWorker.class);
        workerMapping.putMapping(TagConstants.DIV, CssConstants.INLINE_TABLE, DisplayTableTagWorker.class);
        workerMapping.putMapping(TagConstants.DIV, CssConstants.TABLE_CELL, TdTagWorker.class);

        // pseudo elements mapping
        String beforePseudoElemName = CssPseudoElementUtil.createPseudoElementTagName(CssConstants.BEFORE);
        String afterPseudoElemName = CssPseudoElementUtil.createPseudoElementTagName(CssConstants.AFTER);
        workerMapping.putMapping(beforePseudoElemName, SpanTagWorker.class);
        workerMapping.putMapping(afterPseudoElemName, SpanTagWorker.class);
        workerMapping.putMapping(beforePseudoElemName, CssConstants.INLINE_BLOCK, DivTagWorker.class);
        workerMapping.putMapping(afterPseudoElemName, CssConstants.INLINE_BLOCK, DivTagWorker.class);
        workerMapping.putMapping(beforePseudoElemName, CssConstants.BLOCK, DivTagWorker.class);
        workerMapping.putMapping(afterPseudoElemName, CssConstants.BLOCK, DivTagWorker.class);
        // For now behaving like display:block in display:table case is sufficient
        workerMapping.putMapping(beforePseudoElemName, CssConstants.TABLE, DivTagWorker.class);
        workerMapping.putMapping(afterPseudoElemName, CssConstants.TABLE, DivTagWorker.class);
        workerMapping.putMapping(CssPseudoElementUtil.createPseudoElementTagName(TagConstants.IMG), ImgTagWorker.class);

        // custom elements mapping, implementation-specific
        workerMapping.putMapping(PageCountElementNode.PAGE_COUNTER_TAG, PageCountWorker.class);
        workerMapping.putMapping(PageMarginBoxContextNode.PAGE_MARGIN_BOX_TAG, PageMarginBoxWorker.class);

        // New tags
        workerMapping.putMapping("akomantoso", DivTagWorker.class);
        workerMapping.putMapping("bill", DivTagWorker.class);
        workerMapping.putMapping("book", DivTagWorker.class);
        workerMapping.putMapping("chapter", DivTagWorker.class);
        workerMapping.putMapping("coverpage", DivTagWorker.class);
        workerMapping.putMapping("container", DivTagWorker.class);
        workerMapping.putMapping("aknp", PTagWorker.class);
        workerMapping.putMapping("longtitle", DivTagWorker.class);
        workerMapping.putMapping("docstage", PTagWorker.class);
        workerMapping.putMapping("doctype", PTagWorker.class);
        workerMapping.putMapping("docpurpose", PTagWorker.class);
        workerMapping.putMapping("preface", DivTagWorker.class);
        workerMapping.putMapping("preamble", DivTagWorker.class);
        workerMapping.putMapping("formula", DivTagWorker.class);
        workerMapping.putMapping("citations", DivTagWorker.class);
        workerMapping.putMapping("citation", DivTagWorker.class);
        workerMapping.putMapping("recitals", DivTagWorker.class);
        workerMapping.putMapping("recital", DivTagWorker.class);
        workerMapping.putMapping("heading", PTagWorker.class);
        workerMapping.putMapping("num", PTagWorker.class);
        workerMapping.putMapping("tblock", DivTagWorker.class);
        workerMapping.putMapping("proviso", DivTagWorker.class);
        workerMapping.putMapping("aknbody", DivTagWorker.class);
        workerMapping.putMapping("akntitle", DivTagWorker.class);
        workerMapping.putMapping("paragraph", DivTagWorker.class);
        workerMapping.putMapping("subparagraph", DivTagWorker.class);
        workerMapping.putMapping("subsection", DivTagWorker.class);
        workerMapping.putMapping("content", DivTagWorker.class);
        workerMapping.putMapping("list", DivTagWorker.class);
        workerMapping.putMapping("point", DivTagWorker.class);
        workerMapping.putMapping("location", PTagWorker.class);
        workerMapping.putMapping("signature", DivTagWorker.class);
        workerMapping.putMapping("person", PTagWorker.class);
        workerMapping.putMapping("role", PTagWorker.class);
        workerMapping.putMapping("conclusions", DivTagWorker.class);
        workerMapping.putMapping("block", DivTagWorker.class);

    }

    /**
     * Gets the default tag worker mapping.
     *
     * @return the default mapping
     */
    static TagProcessorMapping getCustomTagWorkerMapping() {
        return workerMapping;
    }

    /**
     * Instantiates a new {@link DefaultTagWorkerMapping} instance.
     */
    private CustomTagWorkerMapping() {
    }

}
