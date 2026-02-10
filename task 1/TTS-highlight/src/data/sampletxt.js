export const textByLanguage = {
  "en-US":
    "Vikaspedia is a Ministry of Electronics and Information Technology, Government of India initiative. It is an AI-augmented technology initiative aimed at accelerating the availability and accessibility of digital information in Indian languages to empower citizens and catalyze digital transformation.",

  "en-IN":
    "Vikaspedia is a Ministry of Electronics and Information Technology, Government of India initiative. It is an AI-augmented technology initiative aimed at accelerating the availability and accessibility of digital information in Indian languages to empower citizens and catalyze digital transformation.",

  "hi-IN":
    "विकासपीडिया भारत सरकार के इलेक्ट्रॉनिक्स और सूचना प्रौद्योगिकी मंत्रालय की एक पहल है। यह एक एआई-संवर्धित प्रौद्योगिकी पहल है, जिसका उद्देश्य भारतीय भाषाओं में डिजिटल जानकारी की उपलब्धता और पहुंच को तेज़ करना, नागरिकों को सशक्त बनाना और डिजिटल परिवर्तन को बढ़ावा देना है।",

  "gu-IN":
    "વિકાસપીડિયા ભારત સરકારના ઇલેક્ટ્રોનિક્સ અને માહિતી પ્રૌદ્યોગિકી મંત્રાલયની એક પહેલ છે. આ એક એઆઈ આધારિત પ્રૌદ્યોગિકી પહેલ છે, જેનો હેતુ ભારતીય ભાષાઓમાં ડિજિટલ માહિતીની ઉપલબ્ધતા અને પહોંચને ઝડપી બનાવવાનો, નાગરિકોને સશક્ત બનાવવા અને ડિજિટલ પરિવર્તનને પ્રોત્સાહન આપવાનો છે।",

  "mr-IN":
    "विकासपीडिया ही भारत सरकारच्या इलेक्ट्रॉनिक्स आणि माहिती तंत्रज्ञान मंत्रालयाची एक उपक्रम आहे. ही एक एआय-संवर्धित तंत्रज्ञानावर आधारित उपक्रम असून भारतीय भाषांमध्ये डिजिटल माहितीची उपलब्धता आणि प्रवेश वाढवणे, नागरिकांना सक्षम करणे आणि डिजिटल परिवर्तनाला चालना देणे हा याचा उद्देश आहे।",

  "ta-IN":
    "விகாஸ்பீடியா என்பது இந்திய அரசின் மின்னணு மற்றும் தகவல் தொழில்நுட்ப அமைச்சகத்தின் ஒரு முயற்சியாகும். இது செயற்கை நுண்ணறிவு அடிப்படையிலான தொழில்நுட்ப முயற்சியாக இருந்து, இந்திய மொழிகளில் டிஜிட்டல் தகவல்களின் கிடைப்பையும் அணுகலையும் அதிகரித்து, குடிமக்களை வலுப்படுத்தவும், டிஜிட்டல் மாற்றத்தை ஊக்குவிக்கவும் நோக்கமாகக் கொண்டுள்ளது.",

  "te-IN":
    "వికాస్‌పీడియా భారత ప్రభుత్వ ఎలక్ట్రానిక్స్ మరియు సమాచార సాంకేతిక మంత్రిత్వ శాఖ యొక్క ఒక కార్యక్రమం. ఇది ఏఐ ఆధారిత సాంకేతిక కార్యక్రమంగా, భారతీయ భాషల్లో డిజిటల్ సమాచార లభ్యతను మరియు ప్రాప్తిని వేగవంతం చేసి, పౌరులను సాధికారితం చేయడం మరియు డిజిటల్ మార్పును ప్రోత్సహించడం లక్ష్యంగా పెట్టుకుంది."
};

export const getSampleText = (language) => {
  return textByLanguage[language] || textByLanguage["en-US"];
};
