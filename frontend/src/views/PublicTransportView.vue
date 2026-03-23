<script setup lang="ts">
import { computed, ref } from 'vue'

const origin = ref('Concordia University, Montreal')
const destination = ref('Old Port of Montreal')

const plannerReady = computed(() => origin.value.trim() && destination.value.trim())

function openTransitRoute() {
  if (!plannerReady.value) return

  const base = 'https://www.google.com/maps/dir/'
  const route = `${encodeURIComponent(origin.value)}/${encodeURIComponent(destination.value)}`
  const params = '?travelmode=transit'
  window.open(`${base}${route}${params}`, '_blank', 'noopener,noreferrer')
}

const metroLines = [
  { name: 'Green Line', note: 'Runs through downtown and key east-west destinations.' },
  { name: 'Orange Line', note: 'Connects major north-south stations across the city.' },
  { name: 'Blue Line', note: 'Useful for cross-town travel in the north-central sector.' },
  { name: 'Yellow Line', note: 'Fast link between Montréal and Longueuil.' },
]

const resourceLinks = [
  {
    title: 'STM Metro Service Updates',
    description: 'Check disruptions, closures, and current metro service notices.',
    href: 'https://www.stm.info/en/info/service-updates/metro',
    button: 'View updates',
  },
  {
    title: 'STM Bus Schedules',
    description: 'Browse bus routes and schedules on the official STM site.',
    href: 'https://www.stm.info/en/info/networks/bus',
    button: 'Open schedules',
  },
  {
    title: 'STM Fares',
    description: 'See current fare categories, passes, and ticket options.',
    href: 'https://www.stm.info/en/info/fares',
    button: 'View fares',
  },
  {
    title: 'STM Network Maps',
    description: 'Access maps and official transit network information.',
    href: 'https://www.stm.info/en/info/networks',
    button: 'Open network info',
  },
]
</script>

<template>
  <div class="page">
    <section class="hero-card">
      <div>
        <p class="eyebrow">Citizen mobility tools</p>
        <h1>Public Transit Hub</h1>
        <p class="hero-text">
          Plan trips, review métro lines, and jump directly to official public transportation
          resources for Montréal.
        </p>
      </div>
      <div class="hero-badge">Sprint 3 Transit Feature</div>
    </section>

    <section class="planner-card">
      <div class="section-head">
        <h2>Trip Planner</h2>
        <p>Open a transit route instantly in Google Maps using transit mode.</p>
      </div>

      <div class="planner-grid">
        <label class="field">
          <span>Starting point</span>
          <input v-model="origin" type="text" placeholder="Enter your starting point" />
        </label>

        <label class="field">
          <span>Destination</span>
          <input v-model="destination" type="text" placeholder="Enter your destination" />
        </label>
      </div>

      <div class="planner-actions">
        <button class="action-btn" :disabled="!plannerReady" @click="openTransitRoute">
          Plan Transit Route
        </button>
        <p class="helper-text">
          This opens an external trip planner in a new tab so users can immediately navigate.
        </p>
      </div>
    </section>

    <section class="content-grid">
      <article class="card lines-card">
        <div class="section-head">
          <h2>Metro Line Snapshot</h2>
          <p>A quick overview of the main métro lines available to citizens.</p>
        </div>

        <div class="line-list">
          <div v-for="line in metroLines" :key="line.name" class="line-item">
            <h3>{{ line.name }}</h3>
            <p>{{ line.note }}</p>
          </div>
        </div>
      </article>

      <article class="card resources-card">
        <div class="section-head">
          <h2>Transit Resources</h2>
          <p>Official STM resources and live information links.</p>
        </div>

        <div class="resource-list">
          <div v-for="resource in resourceLinks" :key="resource.title" class="resource-item">
            <div>
              <h3>{{ resource.title }}</h3>
              <p>{{ resource.description }}</p>
            </div>
            <a
              class="secondary-btn"
              :href="resource.href"
              target="_blank"
              rel="noopener noreferrer"
            >
              {{ resource.button }}
            </a>
          </div>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.page {
  padding: 2rem clamp(1rem, 2vw, 2.5rem);
  width: min(96vw, 1200px);
  margin: 0 auto;
}

.hero-card,
.planner-card,
.card {
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
  padding: 1.5rem;
}

.hero-card {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: flex-start;
  margin-bottom: 1.25rem;
}

.eyebrow {
  margin: 0 0 0.35rem 0;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  font-size: 0.75rem;
  color: #7c3aed;
  font-weight: 800;
}

h1 {
  margin: 0;
  color: #1a202c;
  font-size: 2rem;
}

.hero-text,
.section-head p,
.line-item p,
.resource-item p,
.helper-text {
  color: #718096;
  line-height: 1.5;
}

.hero-text {
  max-width: 700px;
  margin-top: 0.75rem;
}

.hero-badge {
  background: #f3e8ff;
  color: #6b21a8;
  padding: 0.7rem 1rem;
  border-radius: 999px;
  font-weight: 700;
  white-space: nowrap;
}

.planner-card {
  margin-bottom: 1.25rem;
}

.section-head h2,
.line-item h3,
.resource-item h3 {
  margin-top: 0;
  color: #2d3748;
}

.planner-grid,
.content-grid {
  display: grid;
  gap: 1rem;
}

.planner-grid {
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  margin-top: 1rem;
}

.content-grid {
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
}

.field {
  display: grid;
  gap: 0.45rem;
}

.field span {
  font-size: 0.9rem;
  font-weight: 700;
  color: #4a5568;
}

.field input {
  border: 1px solid #dbe3ee;
  border-radius: 10px;
  padding: 0.85rem 0.95rem;
  font-size: 0.95rem;
}

.field input:focus {
  outline: none;
  border-color: #7c3aed;
  box-shadow: 0 0 0 3px rgba(124, 58, 237, 0.12);
}

.planner-actions {
  margin-top: 1rem;
}

.action-btn,
.secondary-btn {
  display: inline-block;
  border-radius: 10px;
  font-weight: 700;
  text-decoration: none;
  text-align: center;
}

.action-btn {
  background: #7c3aed;
  color: white;
  border: none;
  padding: 0.85rem 1.15rem;
  cursor: pointer;
}

.action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.secondary-btn {
  background: #edf2f7;
  color: #2d3748;
  border: 1px solid #e2e8f0;
  padding: 0.75rem 1rem;
}

.line-list,
.resource-list {
  display: grid;
  gap: 0.9rem;
  margin-top: 1rem;
}

.line-item,
.resource-item {
  border: 1px solid #edf2f7;
  border-radius: 12px;
  padding: 1rem;
  background: #f8fafc;
}

.resource-item {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  align-items: center;
}

@media (max-width: 720px) {
  .hero-card,
  .resource-item {
    flex-direction: column;
    align-items: stretch;
  }

  .hero-badge {
    white-space: normal;
  }
}
</style>
