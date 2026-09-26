export class ServiceError extends Error {
  constructor(code, status) {
    super(code); this.code = code; this.status = status;
  }
}

function exactKeys(value, keys) {
  return value !== null && typeof value === 'object' && !Array.isArray(value) &&
    Object.keys(value).length === keys.length && keys.every(key => Object.hasOwn(value, key));
}

export function validateInput(value) {
  if (!exactKeys(value, ['text', 'language', 'direction']) ||
      typeof value.text !== 'string' || value.text.length > 1000 || !value.text.trim() ||
      /[\u0000-\u0008\u000b\u000c\u000e-\u001f]/u.test(value.text) ||
      !value.text.isWellFormed() || value.language !== 'en' ||
      !['child_to_adult', 'adult_to_child'].includes(value.direction)) {
    throw new ServiceError('invalid_input', 400);
  }
  return { text: value.text.trim(), language: value.language, direction: value.direction };
}

const useful = (value, min, max) => typeof value === 'string' &&
  value.trim().length >= min && value.length <= max && /[\p{L}\p{N}]/u.test(value) &&
  !/^\s*[\[{]/u.test(value) && !value.includes('```') &&
  !/[\u0000-\u0008\u000b\u000c\u000e-\u001f]/u.test(value) && value.isWellFormed();

export function validateOutput(value) {
  const bad = () => { throw new ServiceError('invalid_output', 502); };
  if (!exactKeys(value, ['translation', 'shortExplanation', 'needsMoreContext', 'clarificationQuestion']) ||
      typeof value.needsMoreContext !== 'boolean' ||
      !['translation', 'shortExplanation', 'clarificationQuestion'].every(key => typeof value[key] === 'string')) bad();
  const result = { translation: value.translation.trim(), shortExplanation: value.shortExplanation.trim(),
    needsMoreContext: value.needsMoreContext, clarificationQuestion: value.clarificationQuestion.trim() };
  if (result.needsMoreContext) {
    if (result.translation || result.shortExplanation || !useful(result.clarificationQuestion, 8, 300)) bad();
  } else {
    if (!useful(result.translation, 2, 2000) || result.clarificationQuestion ||
        (result.shortExplanation && !useful(result.shortExplanation, 2, 600))) bad();
    if (result.shortExplanation.toLowerCase() === result.translation.toLowerCase()) result.shortExplanation = '';
  }
  return result;
}

export const resultSchema = {
  type: 'object', additionalProperties: false,
  properties: {
    translation: { type: 'string' },
    shortExplanation: { type: 'string' },
    needsMoreContext: { type: 'boolean' },
    clarificationQuestion: { type: 'string' }
  },
  required: ['translation', 'shortExplanation', 'needsMoreContext', 'clarificationQuestion']
};
