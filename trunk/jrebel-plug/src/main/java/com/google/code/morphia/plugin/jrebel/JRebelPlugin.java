package com.google.code.morphia.plugin.jrebel;

import java.util.LinkedList;
import java.util.List;

import org.zeroturnaround.javarebel.ClassEventListener;
import org.zeroturnaround.javarebel.ClassResourceSource;
import org.zeroturnaround.javarebel.Plugin;
import org.zeroturnaround.javarebel.ReloaderFactory;

import com.google.code.morphia.Morphia;
import com.google.code.morphia.mapping.Mapper;

public class JRebelPlugin implements Plugin
{

    private static String URL = "http://code.google.com/p/morphia/wiki/JRebel";

    private static JRebelPlugin INSTANCE;

    public static final JRebelPlugin getInstance()
    {
        return INSTANCE;
    }

    List<Mapper> mappers = new LinkedList<Mapper>();

    public JRebelPlugin()
    {
        INSTANCE = this;
    }

    public void applyTo(final Morphia m)
    {
        this.mappers.add(m.getMapper());
    }

    public void preinit()
    {
        ReloaderFactory.getInstance().addClassReloadListener(new ClassEventListener()
        {

            public int priority()
            {
                return 100;
            }

            @SuppressWarnings("rawtypes")
            public synchronized void onClassEvent(final int eventType, final Class klass)
            {
                if (JRebelPlugin.this.mappers.isEmpty())
                {
                    System.err
                            .println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
                    System.err.println("+ " + JRebelPlugin.class.getSimpleName()
                            + " cannot act on reloaded class " + klass.getName());
                    System.err.println("+ Please add " + JRebelPlugin.class.getSimpleName()
                            + ".getInstance() to Morphia as an extension");
                    System.err.println("+ see " + JRebelPlugin.URL);
                    System.err
                            .println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");

                }
                else
                {
                    for (final Mapper m : JRebelPlugin.this.mappers)
                    {
                        if (m.isMapped(klass))
                        {
                            System.out.println(JRebelPlugin.class.getSimpleName()
                                    + ": Remapping reloaded class " + klass.getName());
                            m.addMappedClass(klass);
                        }
                    }
                }

            }
        });
    }

    public boolean checkDependencies(final ClassLoader classLoader, final ClassResourceSource classResourceSource)
    {

        final boolean morphiaAvail = classResourceSource.getClassResource("com.google.code.morphia.Morphia") != null;
        return morphiaAvail;
    }

    public String getId()
    {
        return "morphia";
    }

    public String getName()
    {
        return "JRebel Morphia Plugin";
    }

    public String getDescription()
    {
        return "Remaps Morphia-Mapped classes on reload";
    }

    public String getAuthor()
    {
        return "us<at>thomas-daily.de";
    }

    public String getWebsite()
    {
        return "";
    }
}
