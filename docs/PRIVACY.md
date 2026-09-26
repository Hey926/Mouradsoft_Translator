# Privacy and public-launch review

Status: prototype. Cloud translation is disabled by default on Android and the backend. Direction is an expression setting, **not age verification**. This document is an engineering handoff, not legal certification.

## Data paths

1. Typed or recognized words live in an activity-scoped ViewModel. The app has no persistent history, database, analytics, content logs, or disk HTTP cache. Android backups and transfer of app files are excluded. Redo discards the app’s session references; this is not a claim of forensic memory erasure.
2. On-device recognition uses Android’s on-device recognizer when reported available. Availability is not a guarantee that the selected English model is downloaded or that recognition will succeed. A failure is shown rather than silently choosing a remote service.
3. When only a system recognition service is available, the user receives a remote-processing disclosure and must agree before audio is sent. The provider’s data and retention policies apply. The app never saves recordings or listens in the background. This user choice is not parental consent or an age-assurance system.
4. Live translation sends reviewed text and the selected language/direction to the backend and AI provider. The backend does not write content or provider secrets to logs or storage. It keeps transient IP-based rate counters; deployed proxies, monitoring, infrastructure, and the provider can have independent processing and retention.
5. The client’s live-send disclosure is informational assent for that session. It does not satisfy all child-data, parental consent, or jurisdictional requirements.

## Provider review required before enabling cloud

OpenAI’s [under-18 guidance](https://developers.openai.com/api/docs/guides/safety-checks/under-18-api-guidance) requires zero data retention before processing personal data of children under 13 or the applicable age of digital consent. Review the full current guidance for your intended users and jurisdictions.

OpenAI’s [data controls](https://developers.openai.com/api/docs/guides/your-data) explain that special retention controls require approval and configuration at the organization/project level. Verify the **actual chosen project, endpoint, and model** and their applicable exceptions. `store: false` avoids requesting stored response state; it does **not** establish approved zero retention across the processing path. Neither `CHILD_DATA_REVIEW_COMPLETED=true` nor an in-app checkbox changes provider approval or account controls.

Before setting the review flag, record outside version control:

- Intended audience, jurisdictions, legal basis, required parental consent and age assurance, and reviewer sign-off.
- Provider terms, current minors requirements, approved organization/project controls, model and endpoint eligibility, and evidence that they are actually enabled.
- Retention, deletion, access, incident handling and operator agreements for the complete chain, including speech services, CDN, TLS proxy, hosting, observability, and provider.
- Whether the app’s reminder to avoid private details is adequate for the intended use. A reminder alone cannot prevent children entering personal data.

Use only synthetic, nonpersonal examples for development. Do not run live tests or enable child use until the relevant requirements are met. The default server refuses live startup if the review flag, secret, or model is missing; that technical check cannot validate the substantive review.

## Public-launch work still required

- Obtain qualified privacy/legal review; publish accurate, accessible privacy and retention notices with an operator identity and contact process.
- Complete child-appropriate consent/age-assurance design as required. Do not infer age from direction.
- Conduct age-appropriate safety evaluation and implement suitable filtering, reporting and escalation for high-risk content. The translator prompt and structured validation are useful boundaries, not a fully evaluated moderation system.
- Evaluate negation, literal meanings, regional slang, unfamiliar terms, ambiguity, bullying, sexual/violent text, and prompt-injection attempts using a reviewed synthetic corpus. Model quality has not been measured against a live provider.
- Configure HTTPS, edge request and body limits, abuse protections, deployment-wide quotas, and billing limits. The app has no account system or verified client identity; in-process rate limits alone do not protect a publicly reachable paid endpoint.
- Review proxy identity handling. Do not enable trust for arbitrary `X-Forwarded-For`. Use a known proxy and trusted edge limiting if distinct client limits are needed.
- Disable content logging throughout infrastructure. Decide and disclose unavoidable operational metadata retention. Configure key rotation and incident response. Never ship provider keys in the client.
- Complete Play target-SDK, Families, permissions and Data safety requirements against current policies and actual behavior. Review all third-party SDKs and speech-service behavior.
- Test physical Android devices, accessibility, slow/offline networks, revocation, process death and backgrounding before release.

No public deployment, purchase, account setup, retention approval, or billable AI test is included in the verified work.
