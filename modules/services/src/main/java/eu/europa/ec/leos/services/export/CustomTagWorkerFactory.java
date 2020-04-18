package eu.europa.ec.leos.services.export;

import com.itextpdf.html2pdf.attach.ITagWorker;
import com.itextpdf.html2pdf.attach.ITagWorkerFactory;
import com.itextpdf.html2pdf.attach.ProcessorContext;
import com.itextpdf.html2pdf.css.CssConstants;
import com.itextpdf.html2pdf.exception.TagWorkerInitializationException;
import com.itextpdf.html2pdf.util.TagProcessorMapping;
import com.itextpdf.styledxmlparser.node.IElementNode;

import java.lang.reflect.Constructor;

public class CustomTagWorkerFactory implements ITagWorkerFactory {

    private static final ITagWorkerFactory INSTANCE = new CustomTagWorkerFactory();

    /** The default mapping. */
    private TagProcessorMapping defaultMapping;

    /**
     * Instantiates a new default tag worker factory.
     */
    public CustomTagWorkerFactory() {
        this.defaultMapping = CustomTagWorkerMapping.getCustomTagWorkerMapping();
    }

    /**
     * Gets {@link ITagWorkerFactory} instance.
     * @return default instance that is used if custom tag workers are not configured
     */
    public static ITagWorkerFactory getInstance() {
        return INSTANCE;
    }

    /**
     * Gets the tag worker class for a specific element node.
     *
     * @param mapping the mapping
     * @param tag the element node
     * @return the tag worker class
     */
    private Class<?> getTagWorkerClass(TagProcessorMapping mapping, IElementNode tag) {
        Class<?> tagWorkerClass = null;
        String display = tag.getStyles() != null ? tag.getStyles().get(CssConstants.DISPLAY) : null;
        if (display != null) {
            tagWorkerClass = mapping.getMapping(tag.name(), display);
        }
        if (tagWorkerClass == null) {
            tagWorkerClass = mapping.getMapping(tag.name());
        }
        return tagWorkerClass;
    }

    /**
     * This is a hook method. Users wanting to provide a custom mapping
     * or introduce their own ITagWorkers should implement this method.
     *
     * @param tag the tag
     * @param context the context
     * @return the custom tag worker
     */
    public ITagWorker getCustomTagWorker(IElementNode tag, ProcessorContext context) {
        return null;
    }

    public ITagWorker getTagWorker(IElementNode tag, ProcessorContext context) {
        ITagWorker tagWorker = getCustomTagWorker(tag, context);

        if (tagWorker == null) {
            Class<?> tagWorkerClass = getTagWorkerClass(this.defaultMapping, tag);

            if (tagWorkerClass == null) {
                return null;
            }

            // Use reflection to create an instance
            try {
                Constructor ctor = tagWorkerClass.getDeclaredConstructor(new Class<?>[]{IElementNode.class, ProcessorContext.class});
                ITagWorker res = (ITagWorker) ctor.newInstance(new Object[]{tag, context});
                return res;
            } catch (Exception e) {
                throw new TagWorkerInitializationException(TagWorkerInitializationException.REFLECTION_IN_TAG_WORKER_FACTORY_IMPLEMENTATION_FAILED,
                        tagWorkerClass.getName(), tag.name(), e);
            }
        }

        return tagWorker;
    }

}
