/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package br.ccsl.cogroo.cmdline.postag;

import java.io.FileInputStream;
import java.io.IOException;

import opennlp.tools.cmdline.AbstractCrossValidatorTool;
import opennlp.tools.cmdline.ArgumentParser.OptionalParameter;
import opennlp.tools.cmdline.ArgumentParser.ParameterDescription;
import opennlp.tools.cmdline.CmdLineUtil;
import opennlp.tools.cmdline.TerminateToolException;
import opennlp.tools.cmdline.params.CVParams;
import opennlp.tools.cmdline.postag.POSEvaluationErrorListener;
import opennlp.tools.postag.POSDictionary;
import opennlp.tools.postag.POSSample;
import opennlp.tools.postag.POSTaggerEvaluationMonitor;
import opennlp.tools.util.model.ModelUtil;
import br.ccsl.cogroo.cmdline.postag.POSTaggerCrossValidatorTool.CVToolParams;
import br.ccsl.cogroo.tools.postag.POSTaggerCrossValidator;

public final class POSTaggerCrossValidatorTool
    extends AbstractCrossValidatorTool<POSSample, CVToolParams> {
  
  interface CVToolParams extends CVParams, TrainingParams {
    @ParameterDescription(valueName = "populateDict", description = "If true will add words from the corpus to the dictionary.")
    @OptionalParameter(defaultValue = "false")
    Boolean getPopulateDict();    
  }

  public POSTaggerCrossValidatorTool() {
    super(POSSample.class, CVToolParams.class);
  }

  public String getShortDescription() {
    return "K-fold cross validator for the learnable POS tagger";
  }

  public void run(String format, String[] args) {
    super.run(format, args);

    mlParams = CmdLineUtil.loadTrainingParameters(params.getParams(), false);
    if (mlParams == null) {
      mlParams = ModelUtil.createTrainingParameters(params.getIterations(), params.getCutoff());
    }

    POSTaggerEvaluationMonitor missclassifiedListener = null;
    if (params.getMisclassified()) {
      missclassifiedListener = new POSEvaluationErrorListener();
    }

    POSTaggerCrossValidator validator;
    try {
      // TODO: Move to util method ...
      POSDictionary tagdict = null;
      if (params.getDict() != null) {
        tagdict = POSDictionary.create(new FileInputStream(params.getDict()));
      }

      validator = new POSTaggerCrossValidator(factory.getLang(), mlParams,
          tagdict, params.getNgram(), params.getPopulateDict(), missclassifiedListener);
      
      validator.evaluate(sampleStream, params.getFolds());
    } catch (IOException e) {
      e.printStackTrace();
      throw new TerminateToolException(-1, "IO error while reading training data or indexing data: " + e.getMessage());
    } finally {
      try {
        sampleStream.close();
      } catch (IOException e) {
        // sorry that this can fail
      }
    }

    System.out.println("done");

    System.out.println();

    System.out.println("Accuracy: " + validator.getWordAccuracy());
  }
}
