-- V33 seeds example KNOWLEDGE graph: Immanuel Kant (Wikipedia summaries, en only)

DO $$
DECLARE knowledge_id UUID := (SELECT id FROM briareus_blog_post_types WHERE name = 'KNOWLEDGE');
        root_id UUID := '66000000-0000-4000-8000-000000000021';
        cpr_id UUID := '66000000-0000-4000-8000-000000000022';
        cat_id UUID := '66000000-0000-4000-8000-000000000023';
        cj_id UUID := '66000000-0000-4000-8000-000000000024';
        ti_id UUID := '66000000-0000-4000-8000-000000000025';
        cat_formula_id UUID := '66000000-0000-4000-8000-000000000027';
BEGIN
  IF knowledge_id IS NULL THEN
    RAISE NOTICE 'KNOWLEDGE type not found, skipping Kant seed';
    RETURN;
  END IF;

  INSERT INTO briareus_posts (id, title, content, type_id, language, parent_id, tags, remote_object, createdat, lastupdatedat)
  VALUES (root_id, 'Immanuel Kant', $k$Immanuel Kant (born Emanuel Kant; 22 April 1724 – 12 February 1804) was a German philosopher. Born in Königsberg in the Kingdom of Prussia, he is considered one of the central thinkers of the Enlightenment. His comprehensive and systematic works in epistemology, metaphysics, logic, ethics, aesthetics, political theory, and the philosophy of religion have made him one of the most influential and highly discussed figures in modern Western philosophy.

Kant's philosophy is centered on the human subject and motivated by the desire to secure the possibility of both knowledge and morality against the threats of skepticism and determinism. In the Critique of Pure Reason (1781/1787), Kant argues for transcendental idealism, the doctrine that space and time are mere "forms of intuition" (German: Anschauung) that structure all experience and that we have knowledge only of "appearances" and not of the nature of things in themselves. Kant drew a parallel to the Copernican Revolution in his proposal to think of the objects of experience as conforming to people's spatial and temporal forms of intuition and the categories of the understanding, instead of the traditional method of showing how the mind might conform to its objects.

Kant believed that reason is the source of morality and that the categorical imperative binds all rational agents. He believed that aesthetics arises from a faculty of disinterested judgment. Kant hoped that perpetual peace could be secured through an international federation of republican states and international cooperation. Kant believed that true religion is grounded on morality. The exact nature of his religious views is a matter of dispute.

*Source: Wikipedia [Immanuel Kant](https://en.wikipedia.org/wiki/Immanuel_Kant) - CC BY-SA 3.0*
$k$, knowledge_id, 'en', NULL, '["philosophy","kant","enlightenment","wikipedia"]'::jsonb, '[]'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO briareus_posts (id, title, content, type_id, language, parent_id, tags, remote_object, createdat, lastupdatedat)
  VALUES
    (cpr_id, 'Critique of Pure Reason - Transcendental Idealism', $k$Critique of Pure Reason (German: Kritik der reinen Vernunft; 1781; second edition 1787) is a book by the German philosopher Immanuel Kant, in which the author seeks to determine the limits and scope of metaphysics. Also referred to as Kant's "First Critique", it was followed by his Critique of Practical Reason (1788) and Critique of Judgment (1790). In the preface to the first edition, Kant explains that by a "critique of pure reason" he means a critique "of the faculty of reason in general, in respect of all knowledge after which it may strive independently of all experience" and that he aims to decide on "the possibility or impossibility of metaphysics".

Kant builds on the work of empiricist philosophers such as John Locke and David Hume, as well as rationalist philosophers such as René Descartes, Gottfried Wilhelm Leibniz and Christian Wolff. He expounds new ideas on the nature of space and time, and tries to provide solutions to the skepticism of Hume regarding knowledge of the relation of cause and effect and that of René Descartes regarding knowledge of the external world. This is argued through the transcendental idealism of objects (as appearance) and their form of appearance. Kant regards the former "as mere representations and not as things in themselves", and the latter as "only sensible forms of our intuition, but not determinations given for themselves or conditions of objects as things in themselves".

*Source: Wikipedia [Critique of Pure Reason](https://en.wikipedia.org/wiki/Critique_of_Pure_Reason) - CC BY-SA 3.0*
$k$, knowledge_id, 'en', root_id, '["philosophy","kant","epistemology","wikipedia"]'::jsonb, '[]'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (cat_id, 'Categorical Imperative - Ethics', $k$The categorical imperative (German: Kategorischer Imperativ) is the central philosophical concept in the deontological moral philosophy of Immanuel Kant. Introduced in Kant's 1785 Groundwork of the Metaphysics of Morals, it is a way of evaluating motivations for action. It is best known in its original formulation: "Act only according to that maxim whereby you can at the same time will that it should become a universal law."

According to Kant, rational beings occupy a special place in creation, and morality can be summed up in an imperative, or ultimate commandment of reason, from which all duties and obligations derive. He defines an imperative as any proposition declaring a certain action (or inaction) to be necessary. Hypothetical imperatives apply to someone who wishes to attain certain ends. For example, "I must drink something to quench my thirst" or "I must study to pass this exam." The categorical imperative, on the other hand, commands immediately the maxims one conceives which match its categorical requirements, denoting an absolute, unconditional requirement that must be obeyed in all circumstances and is justified as an end in itself, possessing intrinsic value beyond simply being desirable.

*Source: Wikipedia [Categorical imperative](https://en.wikipedia.org/wiki/Categorical_imperative) - CC BY-SA 3.0*
$k$, knowledge_id, 'en', root_id, '["philosophy","kant","ethics","wikipedia"]'::jsonb, '[]'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (cj_id, 'Critique of Judgment - Aesthetics', $k$The Critique of Judgment (German: Kritik der Urteilskraft), also translated as the Critique of the Power of Judgment, is a 1790 book by the German philosopher Immanuel Kant. Sometimes referred to as the "third critique", the Critique of Judgment follows the Critique of Pure Reason (1781) and the Critique of Practical Reason (1788).

Immanuel Kant's Critique of Judgment is the third critique in Kant's Critical project begun in the Critique of Pure Reason and the Critique of Practical Reason (the First and Second Critiques, respectively). The book is divided into two main sections: the Critique of Aesthetic Judgment and the Critique of Teleological Judgment, and also includes a large overview of the entirety of Kant's Critical system, arranged in its final form. The so-called First Introduction was not published during Kant's lifetime, for Kant wrote a replacement for publication.

*Source: Wikipedia [Critique of Judgment](https://en.wikipedia.org/wiki/Critique_of_Judgment) - CC BY-SA 3.0*
$k$, knowledge_id, 'en', root_id, '["philosophy","kant","aesthetics","wikipedia"]'::jsonb, '[]'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (ti_id, 'Transcendental Idealism', $k$Transcendental idealism is a philosophical system founded by German philosopher Immanuel Kant in the 18th century. Kant's epistemological program is found throughout his Critique of Pure Reason (1781). By transcendental (a term that deserves special clarification) Kant means that his philosophical approach to knowledge transcends mere consideration of sensory evidence (the hallmark of the empiricist philosophers who immediately preceded him) and requires an understanding of the mind's innate modes of processing that sensory evidence.

In the "Transcendental Aesthetic" section of the Critique of Pure Reason, Kant outlines how space and time are pure forms of human intuition contributed by our own faculty of sensibility. Space and time do not have an existence "outside" of us, but are the "subjective" forms of our sensibility and hence the necessary a priori conditions under which the objects we encounter in our experience can appear to us at all. Kant describes time and space not only as "empirically real" but transcendentally ideal.

*Source: Wikipedia [Transcendental idealism](https://en.wikipedia.org/wiki/Transcendental_idealism) - CC BY-SA 3.0*
$k$, knowledge_id, 'en', root_id, '["philosophy","kant","epistemology","wikipedia"]'::jsonb, '[]'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO NOTHING;

  INSERT INTO briareus_posts (id, title, content, type_id, language, parent_id, tags, remote_object, createdat, lastupdatedat)
  VALUES (cat_formula_id, 'Formulas of the Categorical Imperative', $k$The categorical imperative has three key formulations in Kant's Groundwork:

**1. Universal Law:** "Act only according to that maxim whereby you can at the same time will that it should become a universal law."

**2. Humanity as End:** "Act in such a way that you treat humanity, whether in your own person or in the person of any other, never merely as a means to an end, but always at the same time as an end."

**3. Kingdom of Ends:** "Act according to maxims of a universally legislating member of a merely possible kingdom of ends."

Each formula emphasizes a different aspect of the same principle: universalizability, respect for persons, and systematic harmony of rational agents.

*Source: Wikipedia [Categorical imperative - Outline](https://en.wikipedia.org/wiki/Categorical_imperative) - CC BY-SA 3.0*$k$, knowledge_id, 'en', cat_id, '["philosophy","kant","ethics","wikipedia"]'::jsonb, '[]'::jsonb, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
  ON CONFLICT (id) DO NOTHING;
END $$;
