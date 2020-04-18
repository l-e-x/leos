package eu.europa.ec.leos.services.export;

import com.itextpdf.html2pdf.css.CssConstants;
import com.itextpdf.html2pdf.css.apply.ICssApplier;
import com.itextpdf.html2pdf.css.apply.ICssApplierFactory;
import com.itextpdf.html2pdf.exception.CssApplierInitializationException;
import com.itextpdf.html2pdf.util.TagProcessorMapping;
import com.itextpdf.styledxmlparser.node.IElementNode;

public class CustomCssApplierFactory implements ICssApplierFactory {

    private static final ICssApplierFactory INSTANCE = new CustomCssApplierFactory();

    private TagProcessorMapping defaultMapping;

    public CustomCssApplierFactory() {
        defaultMapping = CustomTagCssApplierMapping.getCustomCssApplierMapping();
    }

    public static ICssApplierFactory getInstance() {
        return INSTANCE;
    }

    public ICssApplier getCssApplier(IElementNode tag) {
        ICssApplier cssApplier = getCustomCssApplier(tag);

        if (cssApplier == null) {
            Class<?> cssApplierClass = getCssApplierClass(defaultMapping, tag);
            if (cssApplierClass != null) {
                try {
                    return (ICssApplier) cssApplierClass.newInstance();
                } catch (Exception e) {
                    throw new CssApplierInitializationException(CssApplierInitializationException.ReflectionFailed,
                            cssApplierClass.getName(), tag.name());
                }
            }
        }

        return cssApplier;
    }

    public ICssApplier getCustomCssApplier(IElementNode tag) {
        return null;
    }

    private static Class<?> getCssApplierClass(TagProcessorMapping mapping, IElementNode tag) {
        Class<?> cssApplierClass = null;
        String display = tag.getStyles() != null ? tag.getStyles().get(CssConstants.DISPLAY) : null;
        if (display != null) {
            cssApplierClass = mapping.getMapping(tag.name(), display);
        }
        if (cssApplierClass == null) {
            cssApplierClass = mapping.getMapping(tag.name());
        }
        return cssApplierClass;
    }

}
