interface FeaturePageProps {
  title: string;
  description: string;
  goal: string;
}

/**
 * Placeholder page for an admin feature module.
 *
 * Each feature area states which goal delivers its live data, so the UI never
 * presents fabricated figures as operational truth.
 */
export function FeaturePage({ title, description, goal }: FeaturePageProps) {
  return (
    <section className="page">
      <header className="page__header">
        <h2 className="page__title">{title}</h2>
        <p className="page__subtitle">{description}</p>
      </header>

      <div className="panel">
        <h3 className="panel__title">Not yet implemented</h3>
        <p className="panel__body">
          This module is delivered by <strong>{goal}</strong>. The route,
          layout and API client are already wired so the feature can be
          connected without further structural changes.
        </p>
      </div>
    </section>
  );
}
