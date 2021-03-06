/**
 * Copyright (C) 2012 cogroo <cogroo@cogroo.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cogroo.entities.tree;

import java.util.ArrayList;
import java.util.List;

import org.cogroo.entities.impl.SyntacticTag;

import opennlp.tools.util.Span;
import org.cogroo.tools.checker.rules.model.TagMask;
import org.cogroo.tools.checker.rules.model.TagMask.SyntacticFunction;

public class TextModel {

  private final TagMask VERB;
  private final TagMask SUBJ;
  private Node root;
  // private final Text text;

  public final static String SPAN = "Span: ";
  public final static String SYNTACTIC_FUNCTION = "Synt.: ";
  public final static String CHUNK_FUNCTION = "Chunk.: ";
  public final static String MORPH_FUNCTION = "Morph.: ";

  public TextModel(org.cogroo.entities.Sentence sentence) {
    this.VERB = new TagMask();
    this.VERB.setSyntacticFunction(SyntacticFunction.VERB);
    this.SUBJ = new TagMask();
    this.SUBJ.setSyntacticFunction(SyntacticFunction.SUBJECT);

    root = toNode(sentence);

  }

  public Node getRoot() {
    return root;
  }

  private Span getSpan(List<org.cogroo.entities.Token> tokens) {
    if (tokens != null && tokens.size() > 0) {
      return new Span(tokens.get(0).getSpan().getStart(), tokens
          .get(tokens.size() - 1).getSpan().getEnd());
    }
    return null;
  }

  private Sentence toNode(org.cogroo.entities.Sentence sentence) {
    Sentence sent = new Sentence();
    sent.setLevel(0);
    // Span span = getSpan(sentence.getTokens());

    List<org.cogroo.entities.Chunk> chunks = sentence.getChunks();
    for (org.cogroo.entities.Chunk chunk : chunks) {
      // check if has syntactic function
      if (chunk.getSyntacticTag().match(this.SUBJ)) {
        sent.addElement(toNode(chunk, chunk.getSyntacticTag(), sent));
      } else if (chunk.getSyntacticTag().match(this.VERB)) {
        sent.addElement(toNode(chunk, chunk.getSyntacticTag(), sent));
      } else if (chunk.getTokens().size() > 1) {
        sent.addElement(toNode(chunk, sent));
      } else {
        for (Token c : toNode(chunk.getTokens(), sent)) {
          sent.addElement(c);
        }
      }
    }

    return sent;
  }

  private Chunk toNode(org.cogroo.entities.Chunk chunk, SyntacticTag synt,
      Node parent) {
    Chunk c = new Chunk();
    c.setLevel(parent.getLevel() + 1);
    // Span span = getSpan(chunk.getTokens());
    c.setSyntacticTag(chunk.getSyntacticTag().toVerboseString());
    // c.details.add(SYNTACTIC_FUNCTION +
    // chunk.getSyntacticTag().toVerboseString());
    // c.details.add(SPAN + span);

    // c.children = new ArrayList<Node>();
    if (chunk.getTokens().size() > 1) {
      c.addElement(toNode(chunk, c));
    } else {
      for (Token t : toNode(chunk.getTokens(), c)) {
        c.addElement(t);
      }

      // c.children.addAll(toNode(chunk.getTokens(), c));
    }

    return c;
  }

  private Chunk toNode(org.cogroo.entities.Chunk chunk, Node parent) {
    Chunk c = new Chunk();
    c.setLevel(parent.getLevel() + 1);
    // Span span = getSpan(chunk.getTokens());
    c.setSyntacticTag(chunk.getMorphologicalTag().getClazzE().toString());
    // c.details.add(CHUNK_FUNCTION + chunk.getMorphologicalTag().getClazzE());
    // c.details.add(SPAN + span);

    // c.children = new ArrayList<Node>();
    for (Token t : toNode(chunk.getTokens(), c)) {
      c.addElement(t);
    }
    // c.children.addAll(toNode(chunk.getTokens(), c));
    // c.parent = parent;

    return c;
  }

  private List<Token> toNode(List<org.cogroo.entities.Token> tokenList,
      Node parent) {

    List<Token> t = new ArrayList<Token>();
    for (org.cogroo.entities.Token token : tokenList) {
      t.add(toNode(token, parent));
    }
    return t;
  }

  private Token toNode(org.cogroo.entities.Token token, Node parent) {
    Token t = new Token();
    t.setLevel(3);
    t.setLexeme(token.toString());
    t.setMorphologicalTag(token.getMorphologicalTag().toVerboseString());
    t.setLemma(token.getPrimitive());
    // Span span = token.getSpan();
    // t.details.add(MORPH_FUNCTION +
    // token.getMorphologicalTag().toVerboseString());
    // t.details.add(SPAN + span);
    // t.parent = parent;

    return t;
  }

  // public class Node
  // {
  // protected Node parent;
  // protected String text;
  // protected Span span;
  // protected List<String> details = new ArrayList<String>();
  // protected List<Node> children;
  //
  // public List<Edge> getAllEdges()
  // {
  // List<Edge> edges = new ArrayList<Edge>();
  // if(children != null)
  // {
  // for (Node node : children) {
  // edges.addAll(node.getAllEdges());
  // edges.add(new Edge(this, node));
  // }
  // }
  //
  // return edges;
  // }
  //
  // public String getText()
  // {
  // if(this.details.size() > 0)
  // {
  // StringBuilder sb = new StringBuilder(text);
  // for (String d : details) {
  // sb.append("\n" + d);
  // }
  //
  // return sb.toString();
  // }
  // return text;
  // }
  // }

  // public class Edge
  // {
  // private Node parent;
  // private Node child;
  // public Edge(Node parent, Node child) {
  // super();
  // this.parent = parent;
  // this.child = child;
  // }
  // public Node getParent() {
  // return parent;
  // }
  // public Node getChild() {
  // return child;
  // }
  // }

  public class Text extends Node {

  }

  public class Sentence extends Node {

  }

  public class Chunk extends Node {

  }

  public class Token extends Leaf {

  }

}
