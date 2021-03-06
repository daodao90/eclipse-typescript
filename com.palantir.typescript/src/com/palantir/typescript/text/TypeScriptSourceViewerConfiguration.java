/*
 * Copyright 2013 Palantir Technologies, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.palantir.typescript.text;

import static com.google.common.base.Preconditions.checkNotNull;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.source.DefaultAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;

import com.palantir.typescript.IPreferenceConstants;
import com.palantir.typescript.text.reconciler.PresentationReconciler;
import com.palantir.typescript.text.reconciler.Reconciler;

/**
 * Configures the features of the editor. This is the entry point for features like intelligent
 * double click, auto completion, and syntax highlighting.
 *
 * @author tyleradams
 */
public final class TypeScriptSourceViewerConfiguration extends TextSourceViewerConfiguration {

    private final TypeScriptEditor editor;

    public TypeScriptSourceViewerConfiguration(TypeScriptEditor editor, IPreferenceStore preferenceStore) {
        super(preferenceStore);

        checkNotNull(editor);

        this.editor = editor;
    }

    @Override
    public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
        return new DefaultAnnotationHover();
    }

    @Override
    public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
        return new IAutoEditStrategy[] { new AutoEditStrategy(this.editor, this.fPreferenceStore) };
    }

    @Override
    public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
        checkNotNull(sourceViewer);

        boolean autoActivationEnabled = this.fPreferenceStore.getBoolean(IPreferenceConstants.CONTENT_ASSIST_AUTO_ACTIVATION_ENABLED);
        int autoActivationDelay = this.fPreferenceStore.getInt(IPreferenceConstants.CONTENT_ASSIST_AUTO_ACTIVATION_DELAY);
        String autoActivationTriggers = this.fPreferenceStore.getString(IPreferenceConstants.CONTENT_ASSIST_AUTO_ACTIVATION_TRIGGERS);

        ContentAssistProcessor contentAssistProcessor = new ContentAssistProcessor(this.editor);
        contentAssistProcessor.setCompletionProposalAutoActivationCharacters(autoActivationTriggers);

        ContentAssistant contentAssistant = new ContentAssistant();
        contentAssistant.addCompletionListener(contentAssistProcessor);
        contentAssistant.enableAutoActivation(autoActivationEnabled);
        contentAssistant.setAutoActivationDelay(autoActivationDelay);
        contentAssistant.setContentAssistProcessor(contentAssistProcessor, IDocument.DEFAULT_CONTENT_TYPE);
        contentAssistant.setInformationControlCreator(new MyInformationControlCreator());
        contentAssistant.enableAutoInsert(true);

        return contentAssistant;
    }

    @Override
    public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
        return new ContentFormatter(this.editor, this.fPreferenceStore);
    }

    @Override
    public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
        return new String[] { "//", "" };
    }

    @Override
    public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
        return new IHyperlinkDetector[] { new HyperlinkDetector(this.editor) };
    }

    public IInformationPresenter getOutlinePresenter(TypeScriptSourceViewer typeScriptSourceViewer) {
        checkNotNull(typeScriptSourceViewer);

        InformationPresenter outlinePresenter = new InformationPresenter(this.getOutlinePresenterControlCreator());
        outlinePresenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
        outlinePresenter.setInformationProvider(new InformationProvider(this.editor), IDocument.DEFAULT_CONTENT_TYPE);
        outlinePresenter.setSizeConstraints(50, 20, true, false);

        return outlinePresenter;
    }

    private IInformationControlCreator getOutlinePresenterControlCreator() {
        return new IInformationControlCreator() {
            @Override
            public IInformationControl createInformationControl(Shell parent) {
                int shellStyle = SWT.RESIZE;
                int treeStyle = SWT.V_SCROLL | SWT.H_SCROLL;

                return new TypeScriptOutlineInformationControl(parent, shellStyle, TypeScriptSourceViewerConfiguration.this.editor,
                    treeStyle);
            }
        };
    }

    @Override
    public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
        return new PresentationReconciler();
    }

    @Override
    public IReconciler getReconciler(ISourceViewer sourceViewer) {
        return new Reconciler(this.editor, sourceViewer);
    }

    @Override
    public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
        return new TextHover(sourceViewer, this.editor);
    }

    private static final class MyInformationControlCreator implements IInformationControlCreator {
        @Override
        public IInformationControl createInformationControl(Shell parent) {
            return new DefaultInformationControl(parent);
        }
    }
}
