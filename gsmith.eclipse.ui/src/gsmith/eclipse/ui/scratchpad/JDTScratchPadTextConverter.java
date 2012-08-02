package gsmith.eclipse.ui.scratchpad;

import gsmith.eclipse.ui.UIActivator;

import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJarEntryResource;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;

/**
 * Convert various JDT objects to scratch pad text.
 */
public class JDTScratchPadTextConverter implements IScratchPadTextConverter {
    @Override
    public String getText(Object selection) {
        // .java files
        ICompilationUnit cu = UIActivator.adaptTo(selection, ICompilationUnit.class);
        if (cu != null) {
            return getFullyQualifiedName(cu);
        }

        // .class files
        IClassFile cl = UIActivator.adaptTo(selection, IClassFile.class);
        if (cl != null) {
            return getFullyQualifiedName(cl);
        }

        // types in PackageExplorer
        IType t = UIActivator.adaptTo(selection, IType.class);
        if (t != null) {
            return getFullyQualifiedName(t);
        }

        // fields, constructors, methods
        IMember m = UIActivator.adaptTo(selection, IMember.class);
        if (m != null) {
            String name = getFullyQualifiedName(m.getTypeRoot());
            if (name != null) {
                try {
                    StringBuilder buf = new StringBuilder(name);

                    if (m instanceof IField) {
                        return buf.append('.').append(m.getElementName()).toString();
                    }
                    else if (m instanceof IMethod) {
                        IMethod method = (IMethod)m;
                        // method name, unless it's a constructor
                        if (!method.isConstructor()) {
                            buf.append('.').append(m.getElementName());
                        }
                        buf.append('(');
                        // do the parameters
                        String[] paramNames = method.getParameterNames();
                        String[] paramTypes = method.getParameterTypes();
                        // sometimes there's more paramNames than there are
                        // paramTypes.
                        // Enums in .class files in .jars do that
                        for (int i = 0; i < paramNames.length && i < paramTypes.length; i++) {
                            if (i != 0) {
                                buf.append(',').append(' ');
                            }

                            // the parameter type
                            String paramPkg = Signature.getSignatureQualifier(paramTypes[i]);
                            if (paramPkg != null && paramPkg.length() > 0) {
                                buf.append(paramPkg).append('.');
                            }
                            buf.append(Signature.getSignatureSimpleName(paramTypes[i]));

                            buf.append(' ').append(paramNames[i]);
                        }
                        return buf.append(')').toString();
                    }
                }
                catch (JavaModelException ex) {
                    // just ignore any issues
                }
            }
        }

        // jar in classpath library
        IPackageFragmentRoot pfr = UIActivator.adaptTo(selection, IPackageFragmentRoot.class);
        if (pfr != null) {
            return pfr.getPath().toOSString();
        }

        // package in a jar in classpath library
        IPackageFragment pf = UIActivator.adaptTo(selection, IPackageFragment.class);
        if (pf != null) {
            return pf.getElementName();
        }

        // non-java thing in jar in classpath library
        IJarEntryResource jer = UIActivator.adaptTo(selection, IJarEntryResource.class);
        if (jer != null) {
            return jer.getPackageFragmentRoot().getPath().toOSString() +
                    "!" + jer.getFullPath().toPortableString(); //$NON-NLS-1$
        }

        return null;
    }

    private static String getFullyQualifiedName(ITypeRoot root) {
        return getFullyQualifiedName(root.findPrimaryType());
    }

    private static String getFullyQualifiedName(IType type) {
        return type != null ? type.getFullyQualifiedName() : null;
    }
}